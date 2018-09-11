package groovyx.acme.json;

import groovy.lang.Closure;
import groovy.json.*;

import groovy.io.LineColumnReader;
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.*;
import java.io.Writer;
import java.util.regex.Pattern;

import static groovy.json.JsonTokenType.CLOSE_BRACKET;
import static groovy.json.JsonTokenType.CLOSE_CURLY;
import static groovy.json.JsonTokenType.COLON;
import static groovy.json.JsonTokenType.COMMA;
import static groovy.json.JsonTokenType.NULL;
import static groovy.json.JsonTokenType.OPEN_BRACKET;
import static groovy.json.JsonTokenType.OPEN_CURLY;
import static groovy.json.JsonTokenType.STRING;


public class AcmeJsonParser {

    AcmeJsonPath jpath;
    AcmeJsonChainHandler root;
    AcmeJsonChainHandler current;

    public AcmeJsonParser(){
        root=null;
        current = null;
        jpath = new AcmeJsonPath();
    }


    public AcmeJsonParser each (String path, Closure closure){
        AcmeJsonChainHandler handler = new AcmeJsonChainHandler(closure, path, false);
        if(root==null) {
            root = handler;
        }
        else current.setNext(handler);
        current = handler;
        return this;
    }

    public AcmeJsonParser each (Pattern pattern, Closure closure){
        AcmeJsonChainHandler handler = new AcmeJsonChainHandler(closure, pattern, false);
        if(root==null) {
            root = handler;
        }
        else current.setNext(handler);
        current = handler;
        return this;
    }

    public AcmeJsonParser each (Closure closure){
        AcmeJsonChainHandler handler = new AcmeJsonChainHandler(closure, false);
        if(root==null) {
            root = handler;
        }
        else current.setNext(handler);
        current = handler;
        return this;
    }

    public AcmeJsonParser onValue (Pattern pattern, Closure closure){
        AcmeJsonChainHandler handler = new AcmeJsonChainHandler(closure, pattern, true);
        if(root==null) {
            root = handler;
        }
        else current.setNext(handler);
        current = handler;
        return this;
    }

    public AcmeJsonParser onValue (Closure closure){
        AcmeJsonChainHandler handler = new AcmeJsonChainHandler(closure, true);
        if(root==null) {
            root = handler;
        }
        else current.setNext(handler);
        current = handler;
        return this;
    }

    public AcmeJsonParser onValue (String path, Closure closure){
        AcmeJsonChainHandler handler = new AcmeJsonChainHandler(closure, path, true);
        if(root==null) {
            root = handler;
        }
        else current.setNext(handler);
        current = handler;
        return this;
    }

    public AcmeJsonParser target (Writer w){
        if (root==null) {
            current = new AcmeJsonChainHandler();
            root=current;
        }
        current.setNext(new AcmeJsonWriter(w));
        return this;
    }

    public AcmeJsonParser target (Writer w, boolean indent){
        if (root==null) {
            current = new AcmeJsonChainHandler();
            root=current;
        }
        current.setNext(new AcmeJsonWriter(w, indent));
        return this;
    }

    public AcmeJsonParser target (){
        if (root==null) {
            current = new AcmeJsonChainHandler();
            root=current;
        }

        current.setNext(new AcmeJsonBuilder());
        return this;
    }

    public AcmeJsonParser target (AcmeJsonHandler next){
        if (root==null) {
            root = new AcmeJsonChainHandler();
            current=root;
        }
        current.setNext(next);
        return this;
    }

    public Object parseText(String text) {
        if (text == null || text.length() == 0) {
            throw new IllegalArgumentException("The JSON input text should neither be null nor empty.");
        }
        return parse(new LineColumnReader(new StringReader(text)));
    }





    public Object parse(File file) {
        return parseFile(file, null);
    }


    public Object parse(File file, String charset) {
        return parseFile(file, charset);
    }

    private Object parseFile(File file, String charset) {
        Reader reader = null;
        try {
            if (charset == null || charset.length() == 0) {
                reader = ResourceGroovyMethods.newReader(file);
            } else {
                reader = ResourceGroovyMethods.newReader(file, charset);
            }
            return parse(reader);
        } catch (IOException ioe) {
            throw new JsonException("Unable to process file: " + file.getPath(), ioe);
        } finally {
            if (reader != null) {
                DefaultGroovyMethodsSupport.closeWithWarning(reader);
            }
        }
    }


    public Object parse(URL url) {
        return parseURL(url, null);
    }


    public Object parse(URL url, Map params) {
        return parseURL(url, params);
    }


    public Object parse(Map params, URL url) {
        return parseURL(url, params);
    }

    private Object parseURL(URL url, Map params) {
        Reader reader = null;
        try {
            if (params == null || params.isEmpty()) {
                reader = ResourceGroovyMethods.newReader(url);
            } else {
                reader = ResourceGroovyMethods.newReader(url, params);
            }
            return parse(reader);
        } catch (IOException ioe) {
            throw new JsonException("Unable to process url: " + url.toString(), ioe);
        } finally {
            if (reader != null) {
                DefaultGroovyMethodsSupport.closeWithWarning(reader);
            }
        }
    }


    public Object parse(URL url, String charset) {
        return parseURL(url, null, charset);
    }


    public Object parse(URL url, Map params, String charset) {
        return parseURL(url, params, charset);
    }


    public Object parse(Map params, URL url, String charset) {
        return parseURL(url, params, charset);
    }

    private Object parseURL(URL url, Map params, String charset) {
        Reader reader = null;
        try {
            if (params == null || params.isEmpty()) {
                reader = ResourceGroovyMethods.newReader(url, charset);
            } else {
                reader = ResourceGroovyMethods.newReader(url, params, charset);
            }
            return parse(reader);
        } catch (IOException ioe) {
            throw new JsonException("Unable to process url: " + url.toString(), ioe);
        } finally {
            if (reader != null) {
                DefaultGroovyMethodsSupport.closeWithWarning(reader);
            }
        }
    }

    public Object parse(Reader reader) {
        if(root==null) root = new AcmeJsonChainHandler().setNext(new AcmeJsonBuilder());
        else if(!current.hasNext) current.setNext(new AcmeJsonBuilder());

        JsonLexer lexer = new JsonLexer(reader);

        JsonToken token = lexer.nextToken();
        if (token.getType() == OPEN_CURLY) {
            parseObject(lexer);
        } else if (token.getType() == OPEN_BRACKET) {
            parseArray(lexer);
        } else {
            throw new JsonException(
                    "A JSON payload should start with " + OPEN_CURLY.getLabel() +
                            " or " + OPEN_BRACKET.getLabel() + ".\n" +
                            "Instead, '" + token.getText() + "' was found " +
                            "on line: " + token.getStartLine() + ", " +
                            "column: " + token.getStartColumn()
            );
        }

        return root.getRoot();
    }


    private void parseArray(JsonLexer lexer) {
        //List content = new ArrayList();
        root.onArrayStart(jpath);

        JsonToken currentToken;

        for(int i=0;;i++) {
            currentToken = lexer.nextToken();

            if (currentToken == null) {
                throw new JsonException(
                        "Expected a value on line: " + lexer.getReader().getLine() + ", " +
                                "column: " + lexer.getReader().getColumn() + ".\n" +
                                "But got an unterminated array."
                );
            }

            if (currentToken.getType() == OPEN_CURLY) {
                //AcmeJsonPath.Element e = new AcmeJsonPath.Element(i, null);
                jpath.push(i, null);
                parseObject(lexer);
            } else if (currentToken.getType() == OPEN_BRACKET) {
                jpath.push(new AcmeJsonPath.Element(i, null));
                parseArray(lexer);
            } else if (currentToken.getType().ordinal() >= NULL.ordinal()) {
                Object value = currentToken.getValue();
                //AcmeJsonPath.Element e = new AcmeJsonPath.Element(i,null);
                jpath.push(new AcmeJsonPath.Element(i,null));
                root.onValue(jpath, value);
            } else if (currentToken.getType() == CLOSE_BRACKET) {
                root.onArrayEnd(jpath);
            } else {
                throw new JsonException(
                        "Expected a value, an array, or an object " +
                                "on line: " + currentToken.getStartLine() + ", " +
                                "column: " + currentToken.getStartColumn() + ".\n" +
                                "But got '" + currentToken.getText() + "' instead."
                );
            }

            currentToken = lexer.nextToken();

            if (currentToken == null) {
                throw new JsonException(
                        "Expected " + CLOSE_BRACKET.getLabel() + " " +
                                "or " + COMMA.getLabel() + " " +
                                "on line: " + lexer.getReader().getLine() + ", " +
                                "column: " + lexer.getReader().getColumn() + ".\n" +
                                "But got an unterminated array."
                );
            }

            // Expect a comma for an upcoming value
            // or a closing bracket for the end of the array
            if (currentToken.getType() == CLOSE_BRACKET) {
                jpath.pop();
                root.onArrayEnd(jpath);
                break;
            } else if (currentToken.getType() == COMMA) {
                jpath.pop();
            } else {
                throw new JsonException(
                        "Expected a value or " + CLOSE_BRACKET.getLabel() + " " +
                                "on line: " + currentToken.getStartLine() + " " +
                                "column: " + currentToken.getStartColumn() + ".\n" +
                                "But got '" + currentToken.getText() + "' instead."
                );
            }
        }

    }


    private void parseObject(JsonLexer lexer) {
        //Map content = new HashMap();
        root.onObjectStart(jpath);

        JsonToken previousToken = null;
        JsonToken currentToken = null;

        for(int i=0;;i++) {
            currentToken = lexer.nextToken();

            if (currentToken == null) {
                throw new JsonException(
                        "Expected a String key on line: " + lexer.getReader().getLine() + ", " +
                                "column: " + lexer.getReader().getColumn() + ".\n" +
                                "But got an unterminated object."
                );
            }

            // expect a string key, or already a closing curly brace

            if (currentToken.getType() == CLOSE_CURLY) {
                //return content;
                root.onObjectEnd(jpath);
                break;
            } else if (currentToken.getType() != STRING) {
                throw new JsonException(
                        "Expected " + STRING.getLabel() + " key " +
                                "on line: " + currentToken.getStartLine() + ", " +
                                "column: " + currentToken.getStartColumn() + ".\n" +
                                "But got '" + currentToken.getText() + "' instead."
                );
            }

            String mapKey = (String) currentToken.getValue();
            AcmeJsonPath.Element e = new AcmeJsonPath.Element(i,mapKey);
            jpath.push(e);

            currentToken = lexer.nextToken();

            if (currentToken == null) {
                throw new JsonException(
                        "Expected a " + COLON.getLabel() + " " +
                                "on line: " + lexer.getReader().getLine() + ", " +
                                "column: " + lexer.getReader().getColumn() + ".\n" +
                                "But got an unterminated object."
                );
            }

            // expect a colon between the key and value pair

            if (currentToken.getType() != COLON) {
                throw new JsonException(
                        "Expected " + COLON.getLabel() + " " +
                                "on line: " + currentToken.getStartLine() + ", " +
                                "column: " + currentToken.getStartColumn() + ".\n" +
                                "But got '" + currentToken.getText() + "' instead."
                );
            }

            currentToken = lexer.nextToken();

            if (currentToken == null) {
                throw new JsonException(
                        "Expected a value " +
                                "on line: " + lexer.getReader().getLine() + ", " +
                                "column: " + lexer.getReader().getColumn() + ".\n" +
                                "But got an unterminated object."
                );
            }

            // value can be an object, an array, a number, string, boolean or null values

            if (currentToken.getType() == OPEN_CURLY) {
                //content.put(mapKey, parseObject(lexer));
                parseObject(lexer);
            } else if (currentToken.getType() == OPEN_BRACKET) {
                //content.put(mapKey, parseArray(lexer));
                parseArray(lexer);
            } else if (currentToken.getType().ordinal() >= NULL.ordinal()) {
                Object value = currentToken.getValue();
                root.onValue(jpath, value);
            } else {
                throw new JsonException(
                        "Expected a value, an array, or an object " +
                                "on line: " + currentToken.getStartLine() + ", " +
                                "column: " + currentToken.getStartColumn() + ".\n" +
                                "But got '" + currentToken.getText() + "' instead."
                );
            }

            previousToken = currentToken;
            currentToken = lexer.nextToken();

            // premature end of the object

            if (currentToken == null) {
                throw new JsonException(
                        "Expected " + CLOSE_CURLY.getLabel() + " or " + COMMA.getLabel() + " " +
                                "on line: " + previousToken.getEndLine() + ", " +
                                "column: " + previousToken.getEndColumn() + ".\n" +
                                "But got an unterminated object."
                );
            }

            // Expect a comma for an upcoming key/value pair
            // or a closing curly brace for the end of the object
            if (currentToken.getType() == CLOSE_CURLY) {
                jpath.pop();
                root.onObjectEnd(jpath);
                break;
            } else if (currentToken.getType() == COMMA){
                jpath.pop();
            } else {
                throw new JsonException(
                        "Expected a value or " + CLOSE_CURLY.getLabel() + " " +
                                "on line: " + currentToken.getStartLine() + ", " +
                                "column: " + currentToken.getStartColumn() + ".\n" +
                                "But got '" + currentToken.getText() + "' instead."
                );
            }
        }

    }




}
