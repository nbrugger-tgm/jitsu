package com.niton;

import com.niton.parser.*;
import com.niton.parser.GrammarResult;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.*;
import com.niton.parser.result.*;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static javax.lang.model.element.Modifier.*;

/**
 * This is the JPGenerator Class
 *
 * @author Nils Brugger
 * @version 2019-06-04
 */
public class JPGenerator {
    private int unnamedIndex = 0;
    private String pack;
    private String path;
    private boolean returnTokens = true;
    private String tokenClassName = "CustomTokens";

    public void setTokenClassName(String tokenClassName) {
        this.tokenClassName = tokenClassName;
    }

    public JPGenerator(String pack, String path) {
        super();
        this.pack = pack;
        this.path = path;
    }

    public JPGenerator() {
        this("com.niton.jainparse.gen", null);
    }

    /**
     * @param returnTokens the returnTokens to set
     */
    public void setReturnTokens(boolean returnTokens) {
        this.returnTokens = returnTokens;
    }

    /**
     * @return the returnTokens
     */
    public boolean isReturningTokens() {
        return returnTokens;
    }

    /**
     * @return the pack
     */
    public String getPack() {
        return pack;
    }

    /**
     * @param pack the pack to set
     */
    public void setPack(String pack) {
        this.pack = pack;
    }

    public void generate(GrammarReference reference, Tokenable[] tokenable) throws IOException, ParsingException {
        unnamedIndex = 1;
        if(tokenable.length > 0)
            generateTokenFile(tokenable);
        for (String g : reference.grammarNames()) {
            generateClass(reference.get(g), reference);
        }
    }

    private void generateClass(Grammar g, GrammarReference ref) throws IOException {
        if (g instanceof ChainGrammar) {
            if (g.getName() == null) {
                g.setName("inline" + unnamedIndex);
                unnamedIndex++;
            }
            generateChainGrammarClass((ChainGrammar) g, ref);
        }
    }

    private void generateChainGrammarClass(ChainGrammar grammar, GrammarReference ref) throws IOException {
        FieldSpec wrappedResult = FieldSpec.builder(SuperGrammarResult.class, "result").addModifiers(PRIVATE).build();
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addStatement("this.result = res")
                .addParameter(SuperGrammarResult.class, "res")
                .addModifiers(PUBLIC).build();

        LinkedList<MethodSpec> getter = new LinkedList<>();
        for (Map.Entry<Integer, String> namedProperty : grammar.getNaming().entrySet()) {
            String propertyName = namedProperty.getValue();
            Grammar subGrammar = grammar.getChain().get(namedProperty.getKey());

            if (!(subGrammar instanceof GrammarReferenceGrammar))
                generateClass(subGrammar, ref);
            subGrammar = resolve(subGrammar, ref);
            TypeName returnType = getReturnType(subGrammar,ref);


            if (subGrammar instanceof IgnoreGrammar)
                continue;
            // Single object return + type unkown
            if (subGrammar instanceof MultiGrammar) {
                getter.add(
                        MethodSpec.methodBuilder("get" + javaFy(propertyName))
                                .addModifiers(PUBLIC).returns(returnType)
                                .addStatement("ResultResolver.setResolveAny("+!returnType.toString().equals(TypeName.get(AnyGrammarResult.class).toString())+")")
                                .addStatement("return ($T) $T.getReturnValue(result.getObject($S))", returnType, ResultResolver.class,propertyName)
                                .build()
                );
            } else if (subGrammar instanceof RepeatGrammar) {
                CodeBlock methodContent = CodeBlock.builder()
                        .addStatement("return (($T)$T.getReturnValue(result.getObject($S)))", returnType,ResultResolver.class, propertyName)
                        .build();

                getter.add(MethodSpec.methodBuilder("get" + javaFy(propertyName))
                        .returns(returnType)
                        .addModifiers(PUBLIC)
                        .addCode(methodContent)
                        .build());
            } else if ((subGrammar instanceof TokenGrammar||subGrammar instanceof AnyExceptGrammar) && returnTokens) {
                getter.add(MethodSpec.methodBuilder("get" + javaFy(propertyName))
                        .returns(returnType)
                        .addModifiers(PUBLIC)
                        .beginControlFlow("if(result.getObject($S) == null)", propertyName)
                        .addStatement("return null")
                        .endControlFlow()
                        .addStatement("return (($T)$T.getReturnValue(result.getObject($S)))", returnType,ResultResolver.class, propertyName)
                        .build());
            } else if (subGrammar instanceof TokenGrammar||subGrammar instanceof AnyExceptGrammar) {
                getter.add(MethodSpec.methodBuilder("get" + javaFy(propertyName))
                        .returns(returnType)
                        .addModifiers(PUBLIC)
                        .addStatement("return ($T) result.getObject($S)", returnType, propertyName)
                        .build());
            } else if (subGrammar instanceof ChainGrammar) {
                getter.add(MethodSpec.methodBuilder("get" + javaFy(propertyName))
                        .returns(returnType)
                        .addModifiers(PUBLIC)
                        .addStatement("return new $T(result.getObject($S))", returnType, propertyName)
                        .build());
            }
        }

        Builder build = TypeSpec
                .classBuilder(javaFy(grammar.getName()))
                .addModifiers(PUBLIC)
                .addField(wrappedResult)
                .addMethod(constructor);

        for (MethodSpec methodSpec : getter) {
            build.addMethod(methodSpec);
        }
        TypeSpec type = build.build();
        JavaFile javaFile = JavaFile.builder(pack, type).indent("\t").build();
        javaFile.writeTo(new File(path));
        System.out.println("Generated : " + javaFile.packageName + "." + javaFile.typeSpec.name);
    }

    private void generateTokenFile(Tokenable[] tokenable) throws IOException {
        Builder tokenFile = TypeSpec
                .enumBuilder(tokenClassName)
                .addSuperinterface(Tokenable.class)
                .addMethod(MethodSpec.methodBuilder("pattern")
                        .returns(String.class)
                        .addModifiers(PUBLIC)
                        .addStatement("return pattern")
                        .build())
                .addField(FieldSpec.builder(String.class, "pattern").addModifiers(FINAL).build())
                .addMethod(MethodSpec.constructorBuilder().addParameter(String.class, "pattern").addStatement("this.pattern = pattern").build());
        for (Tokenable t : tokenable)
            tokenFile = tokenFile.addEnumConstant(t.name(), TypeSpec.anonymousClassBuilder("$S", t.pattern()).build());

        JavaFile javaFile = JavaFile.builder(pack, tokenFile.build()).indent("\t").build();
        javaFile.writeTo(new File(path));
        System.out.println("Generated : " + javaFile.packageName + "." + javaFile.typeSpec.name);
    }

    public Grammar resolve(Grammar g, GrammarReference ref) {
        while (g instanceof GrammarReferenceGrammar || g instanceof OptionalGrammar) {
            if (g instanceof GrammarReferenceGrammar){
                String name =((GrammarReferenceGrammar) g).getGrammar();
                g = ref.get(name);
                if(g == null)
                    throw new IllegalArgumentException("The grammar \'"+name+"\' was ot found in the reference");
            }
            else
                g = ((OptionalGrammar) g).getCheck();
        }
        return g;
    }

    public TypeName getReturnType(Grammar g,GrammarReference ref) {
        if (g instanceof ChainGrammar)
            return ClassName.get(pack, javaFy(g.getName()));
        if (g instanceof AnyExceptGrammar)
            return ClassName.get(String.class);
        if (g instanceof MultiGrammar) {
            if (((MultiGrammar) g).getGrammars().length > 0)
                if (Arrays.stream(((MultiGrammar) g).getGrammars()).map(gr -> getReturnType(resolve(gr,ref),ref).toString()).distinct().count() == 1)
                    return getReturnType(resolve(((MultiGrammar) g).getGrammars()[0], ref),ref);
            return ClassName.get(AnyGrammarResult.class);
        }
        if (g instanceof OptionalGrammar)
            return getReturnType(((OptionalGrammar) g).getCheck(),ref);
        if (g instanceof TokenGrammar)
            return ClassName.get(String.class);
        if (g instanceof RepeatGrammar)
            return ParameterizedTypeName.get(ClassName.get(List.class), getReturnType(resolve(((RepeatGrammar) g).getCheck(), ref),ref));
        return null;
    }



    public GrammarResult resolve(GrammarResult res) {
        while (res != null && res instanceof OptionalGrammarResult) {
            res = ((OptionalGrammarResult) res).getValue();
        }
        return res;
    }

    public List<Object> resolveList(ListGrammarResult res) {
        List<Object> list = new ArrayList<>();
        for (GrammarResult obj : res.getList()) {
            obj = resolve(obj);
            if (obj instanceof ListGrammarResult)
                list.add(resolveList((ListGrammarResult) obj));
            else
                list.add(obj);
        }
        return list;
    }

    public String javaFy(String s) {
        StringBuilder newName =  new StringBuilder();
        boolean start = true;
        for (int i = 0;i<s.length();i++){
            char c = s.charAt(i);
            if(c == '_' || c == '-')
                start = true;
            else {
                c = start ? Character.toUpperCase(c) : Character.toLowerCase(c);
                newName.append(c);
                start = false;
            }
        }
        return newName.toString();
    }
}
