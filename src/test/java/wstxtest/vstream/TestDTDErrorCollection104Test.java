package wstxtest.vstream;

import stax2.BaseStax2Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.ValidationProblemHandler;
import org.codehaus.stax2.validation.XMLValidationException;
import org.codehaus.stax2.validation.XMLValidationProblem;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidationSchemaFactory;

public class TestDTDErrorCollection104Test
    extends BaseStax2Test
{
    // [woodstox-core#103]
    public void testValidationBeyondUnknownElement() throws Exception
    {
        final String DOC =
                "<map>\n" + 
                "  <val>\n" + 
                "    <prop att=\"product\" val=\"win\" action=\"flag\" color=\"black\"/>\n" +
                "  </val>\n" + 
                "</map>\n";

        final String INPUT_DTD =
"<!ELEMENT map (notval+)>\n"
+"<!ELEMENT notval EMPTY>\n"
;

        XMLInputFactory f = getInputFactory();
        setCoalescing(f, true);

        XMLValidationSchemaFactory schemaFactory =
                XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_DTD);
        XMLValidationSchema schema = schemaFactory.createSchema(new StringReader(INPUT_DTD));
        XMLStreamReader2 sr = (XMLStreamReader2)f.createXMLStreamReader(
                new StringReader(DOC));

        final List<XMLValidationProblem> probs = new ArrayList<XMLValidationProblem>();
        
        sr.validateAgainst(schema);
        sr.setValidationProblemHandler(new ValidationProblemHandler() {
            @Override
            public void reportProblem(XMLValidationProblem problem)
                    throws XMLValidationException {
                probs.add(problem);
            }
        });

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("map", sr.getLocalName());

        sr.next(); // SPACE or CHARACTERS
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("val", sr.getLocalName());
        assertEquals(1, probs.size());
        assertEquals("Undefined element <val> encountered", 
                probs.get(0).getMessage());

        sr.next(); // SPACE or CHARACTERS
        assertEquals(1, probs.size());

        // From this point on, however, behavior bit unclear except
        // that for DTD I guess we can at least check for undefined
        // cases....
        
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("prop", sr.getLocalName());
        // <prop> not defined either so:
        assertEquals(2, probs.size());
        assertEquals("Undefined element <prop> encountered", 
                probs.get(1).getMessage());

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("prop", sr.getLocalName());
        assertEquals(2, probs.size());

        sr.next(); // SPACE or CHARACTERS
        assertEquals(2, probs.size());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("val", sr.getLocalName());
        assertEquals(2, probs.size());
        
        sr.next(); // SPACE or CHARACTERS
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("map", sr.getLocalName());
        assertEquals(3, probs.size());
        assertEquals("Validation error, element </map>: Expected at least one element <notval>", 
                probs.get(2).getMessage());

        // Plus content model now missing <notval>(s)
        assertTokenType(END_DOCUMENT, sr.next());
        assertEquals(3, probs.size());

        sr.close();
    }
}
