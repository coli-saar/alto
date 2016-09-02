/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.util.CpuTimeStopwatch;
import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import com.google.common.reflect.ClassPath;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.util.MutableInteger;
import de.up.ling.irtg.util.Util;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author groschwitz
 */
public class Program {

    private final Object2IntMap<String> varName2Index;
    private final String[] varNames;
    private final boolean[] isGlobal;
    private final boolean[] neededForGlobal;
    private final boolean[] isInitialization;
    private final boolean[] isNumeric;
    private final boolean[] doExport;
    private final Object[] variableTracker;
    private final Object[] globalVariableTracker;
    private final List<String> allWatchNames;//for convenience
    private final Int2ObjectMap<List<String>> watchesStartingBefore;
    private final Int2ObjectMap<List<String>> watchesStoppingAfter;
    private final Object2IntMap<String> interp2Index;
    private final List<String> additionalData;
    private final List<Tree<Operation>> program;
    private final int interpCount;
    private int numThreads;
    private String firstMentionedInterpretation = null; // first interpretation in the program that is mentioned as input, i.e. "<interpretation>"

    public static final String BASE_IRTG_SYMBOL = "$";
    public static final String ADDITIONAL_DATA_SYMBOL = "#";
    public static final String LEFT_INPUT_DELIMITER = "<";
    public static final String RIGHT_INPUT_DELIMITER = ">";
    public static final String LEFT_STRING_DELIMITER = "__";
    public static final String RIGHT_STRING_DELIMITER = "__";
    public static final String NULL_SYMBOL = "**null**";
    public static final String GET_INTERP_CODE = "interp";
    public static final String EXPORT_CODE = "export";
    public static final String GLOBAL_CODE = "global";
    public static final String INITIALIZATION_CODE = "init";
    public static final String INITIALIZE_CODE = "global";
    public static final String START_WATCH_CODE = "starttime";
    public static final String STOP_WATCH_CODE = "stoptime";
    public static final String ASSIGNMENT_PATTERN = " ?= ?";
    public static final String COMMENT_SYMBOL = "//";
    public static final Method GET_ALG_METHOD = getGetAlgMethod();//necessary for getting return type

    public static final String VERBOSE_ALL = "ALL";

    public Program(InterpretedTreeAutomaton irtg, List<String> additionalData, List<String> unparsedProgram, Map<String, String> varRemapper) throws VariableNotDefinedException {

        //preprocessing (currently the watches and varibale remapping)
        interpCount = irtg.getInterpretations().size();
        watchesStartingBefore = new Int2ObjectOpenHashMap<>();
        watchesStoppingAfter = new Int2ObjectOpenHashMap<>();
        allWatchNames = new ArrayList<>();
        List<String> preprocessedProgram = preprocessCode(unparsedProgram, varRemapper);

        //setup of rest
        int length = preprocessedProgram.size();
        if (additionalData == null) {
            this.additionalData = new ArrayList<>();
        } else {
            this.additionalData = additionalData;
        }
        isGlobal = new boolean[shiftIndex(length)];//gets initialized as false, which is not completely correct (see next lines)
        isGlobal[0] = true;//the base IRTG is global (that is, not dependent on the Corpus)
        for (int i = 0; i < this.additionalData.size(); i++) {
            isGlobal[shiftAdditionalDataIndex(i)] = true;
        }
        isInitialization = new boolean[shiftIndex(length)];//gets initialized as false, which correct
        isNumeric = new boolean[shiftIndex(length)];//gets initialized as false, which is correct (neither IRTG nor input or additional data are handled as numeric)
        doExport = new boolean[shiftIndex(length)];//gets initialized as false, which is correct
        neededForGlobal = new boolean[shiftIndex(length)];//gets initialized as false, which is correct
        varNames = new String[length];
        varName2Index = new Object2IntOpenHashMap<>();
        variableTracker = new Object[shiftIndex(length)];
        globalVariableTracker = new Object[shiftIndex(length)];
        program = new ArrayList<>();
        interp2Index = new Object2IntOpenHashMap<>();
        
        //parse the code
        parseProgram(getTreesFromCommandCode(preprocessedProgram), irtg);
//        System.err.println("parsed program: ");
//        for (Tree<Operation> tree : program) {
//            System.err.println(tree);
//        }
        
        //System.err.println("parsed: "+program);
    }

    static Method getGetAlgMethod() {
        try {
            return Interpretation.class.getMethod("getAlgebra", new Class[0]);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Map<String, Method> getAllAnnotatedStaticMethods() {

        //for some reason reflections.getMethodsAnnotatedWith(OperationAnnotation.class) does not seem to work with a MethodAnnotationsScanner, so we are getting all classes
        Map<String, Method> ret = new HashMap<>();
        for (Class clazz : getAllClassesIn_irtg_Classpath()) {
            for (Method m : clazz.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getAnnotation(OperationAnnotation.class) != null) {
                    OperationAnnotation annot = m.getAnnotation(OperationAnnotation.class);
                    String code = annot.code();
                    ret.put(code, m);
                }
            }
        }

        return ret;
    }

    private static Set<Class> getAllClassesIn_irtg_Classpath() {
        Set<Class> ret = new HashSet<>();
        /*Reflections reflections = new Reflections(new ConfigurationBuilder()
         .setUrls(ClasspathHelper.forJavaClassPath())
         .setScanners(new MethodAnnotationsScanner()));*/
        /*Reflections reflections = new Reflections("de.up.ling.irtg.corpus", new SubTypesScanner(false));
         Set<String> classes = reflections.getStore().getSubTypesOf(Object.class.getName());
         System.err.println(classes);*/
        try {
            ClassPath cPath = ClassPath.from(ClassLoader.getSystemClassLoader());
            for (ClassPath.ClassInfo info : cPath.getTopLevelClassesRecursive("de.up.ling.irtg")) {
                try {
                    ret.add(Class.forName(info.getName()));
                } catch (ClassNotFoundException ex) {
                    System.err.println("ERROR: class '" + info.getName() + "' not found!");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ret;
    }

    private static Map<String, Constructor> getAllAnnotatedConstructors() {
        //for some reason reflections.getConstructorsAnnotatedWith(OperationAnnotation.class) does not seem to work, so we are getting all classes
        Map<String, Constructor> ret = new HashMap<>();
        for (Class clazz : getAllClassesIn_irtg_Classpath()) {
            for (Constructor m : clazz.getConstructors()) {
                if (m.getAnnotation(OperationAnnotation.class) != null) { //Modifier.isStatic(m.getModifiers()) &&
                    OperationAnnotation annot = (OperationAnnotation) m.getAnnotation(OperationAnnotation.class);
                    String code = annot.code();
                    ret.put(code, m);
                }
            }
        }
        return ret;
    }

    private int shiftIndex(int i) {
        return 1 + interpCount + additionalData.size() + i;//the 1 for the irtg
    }

    private int shiftAdditionalDataIndex(int i) {
        return 1 + interpCount + i;//the 1 for the irtg
    }

    /**
     * Processes all lines of code that are not actually operations (currently
     * only timer commands) and returns a list of all operation-lines, i.e the
     * actual program.
     *
     * @param input
     * @return
     */
    private List<String> preprocessCode(List<String> input, Map<String, String> varRemapper) throws VariableNotDefinedException {
        List<String> ret = new ArrayList<>();

        for (String commandLine : input) {
            int posInProgram = ret.size();

            if (!"".equals(commandLine.trim()) && !commandLine.startsWith(COMMENT_SYMBOL)) {

                if (commandLine.startsWith(START_WATCH_CODE + " ")) {
                    List<String> watchesStartingHere = watchesStartingBefore.get(posInProgram);
                    if (watchesStartingHere == null) {
                        watchesStartingHere = new ArrayList<>();
                        watchesStartingBefore.put(posInProgram, watchesStartingHere);
                    }
                    watchesStartingHere.add(commandLine.substring(START_WATCH_CODE.length() + 1));
                } else if (commandLine.startsWith(STOP_WATCH_CODE + " ")) {
                    String watchName = commandLine.substring(STOP_WATCH_CODE.length() + 1);
                    allWatchNames.add(watchName);
                    List<String> watchesStoppingHere = watchesStoppingAfter.get(posInProgram - 1);
                    if (watchesStoppingHere == null) {
                        watchesStoppingHere = new ArrayList<>();
                        watchesStoppingAfter.put(posInProgram - 1, watchesStoppingHere);
                    }
                    watchesStoppingHere.add(watchName);
                } else {
                    String[] varAndCommand = commandLine.split(ASSIGNMENT_PATTERN);
                    if (varAndCommand.length == 1) {
                        String[] keywordsAndVarName = commandLine.trim().split(" ");
                        String remappedCode = varRemapper.get(keywordsAndVarName[keywordsAndVarName.length - 1]);
                        if (remappedCode == null) {
                            throw new VariableNotDefinedException(keywordsAndVarName[keywordsAndVarName.length - 1]);
                        } else {
                            ret.add(commandLine + ASSIGNMENT_PATTERN.replace("?", "") + remappedCode);
                        }
                    } else {
                        ret.add(commandLine);
                    }

                }
            }
        }
        return ret;
    }

    /**
     * Takes the raw code string (a list of the lines) and returns the
     * corresponding command trees (the commands are still strings).
     *
     * @param input
     * @return
     */
    private List<Tree<String>> getTreesFromCommandCode(List<String> input) {
        List<Tree<String>> ret = new ArrayList<>();

        for (int i = 0; i < input.size(); i++) {
            String commandLine = input.get(i);
            if (commandLine.startsWith(EXPORT_CODE + " ")) {
                doExport[shiftIndex(i)] = true;
                commandLine = commandLine.replaceFirst(EXPORT_CODE + " ", "");
            }

            //the following two codes are mutually exclusive, everything in init is also global
            if (commandLine.startsWith(INITIALIZATION_CODE + " ")) {
                isInitialization[shiftIndex(i)] = true;
                isGlobal[shiftIndex(i)] = true;
                commandLine = commandLine.replaceFirst(INITIALIZATION_CODE + " ", "");
            } else if (commandLine.startsWith(GLOBAL_CODE + " ")) {
                isGlobal[shiftIndex(i)] = true;
                commandLine = commandLine.replaceFirst(GLOBAL_CODE + " ", "");
            }

            String[] varAndCommand = commandLine.split(ASSIGNMENT_PATTERN);
            String varName = varAndCommand[0];
            varName2Index.put(varName, i);
            varNames[i] = varName;
            String command = varAndCommand[1];
            DottedCommandLexer lexer = new DottedCommandLexer(new ANTLRInputStream(command));
            DottedCommandParser p = new DottedCommandParser(new CommonTokenStream(lexer));
            DottedCommandParser.ExprContext tree = p.expr();

            ParseTreeWalker walker = new ParseTreeWalker();

            Map<Object, Tree<String>> parses = new HashMap<>();
            Map<DottedCommandParser.ArgumentContext, List<Tree<String>>> arg2Children = new HashMap<>();

            DottedCommandListener listener = new DottedCommandBaseListener() {

                @Override
                public void exitExpr(DottedCommandParser.ExprContext ctx) {
                    ParseTree child0 = ctx.getChild(0);
                    if (child0 instanceof DottedCommandParser.DottedCommandContext) {
                        List<Tree<String>> childrenHere = new ArrayList<>();
                        childrenHere.add(parses.get(((DottedCommandParser.DottedCommandContext) child0).getChild(0)));
                        childrenHere.addAll(arg2Children.get((DottedCommandParser.ArgumentContext) ctx.getChild(2)));
                        parses.put(ctx, Tree.create(child0.getChild(2).getText(), childrenHere));
                    } else if (child0 instanceof TerminalNode) {
                        if (ctx.children.size() == 1) {
                            parses.put(ctx, Tree.create(((TerminalNode) child0).getSymbol().getText(), new ArrayList<>()));
                        } else {
                            parses.put(ctx, Tree.create(child0.getText(), arg2Children.get((DottedCommandParser.ArgumentContext) ctx.getChild(2))));
                        }
                    } else if (child0 instanceof DottedCommandParser.DottedExprContext) {
                        parses.put(ctx, parses.get(child0));
                    }

                }

                /*@Override
                 public void exitCommand(DottedCommandParser.CommandContext ctx) {
                 ParseTree child0 = ctx.children.get(0);
                 if (child0 instanceof DottedCommandParser.DottedExprContext) {
                 String label = ctx.children.get(2).getText();
                 parses.put(ctx, parses.get(ctx.children.get(0)));
                 comm2Label.put(ctx, label);
                 } else if (child0 instanceof TerminalNode) {
                 comm2Label.put(ctx, ctx.children.get(0).getText());
                 }
                 }*/
                @Override
                public void exitDottedExpr(DottedCommandParser.DottedExprContext ctx) {
                    ParseTree child0 = ctx.children.get(0);
                    if (child0 instanceof DottedCommandParser.DottedExprContext) {
                        ParseTree child2 = ctx.children.get(2);
                        if (child2 instanceof TerminalNode) {
                            String label = ((TerminalNode) child2).getSymbol().getText();
                            parses.put(ctx, Tree.create(label, Collections.singletonList(parses.get(child0))));
                        } else if (child2 instanceof DottedCommandParser.InterpretationContext) {
                            String label = GET_INTERP_CODE;
                            Tree<String> rightChild = Tree.create(LEFT_STRING_DELIMITER + child2.getChild(1).getText() + RIGHT_STRING_DELIMITER, new ArrayList<>());
                            List<Tree<String>> childrenHere = new ArrayList<>();
                            childrenHere.add(parses.get(child0));
                            childrenHere.add(rightChild);
                            parses.put(ctx, Tree.create(label, childrenHere));
                        }
                    } else if (child0 instanceof TerminalNode) {
                        String label = ((TerminalNode) child0).getSymbol().getText();
                        parses.put(ctx, Tree.create(label, new ArrayList<>()));
                    }
                }

                @Override
                public void visitTerminal(TerminalNode tn) {
                    parses.put(tn, Tree.create(tn.getSymbol().getText(), new ArrayList<>()));
                }

                @Override
                public void exitArgument(DottedCommandParser.ArgumentContext ctx) {
                    if (ctx.children.size() > 1) {
                        List<Tree<String>> childrenHere = new ArrayList<>();
                        childrenHere.add(parses.get(ctx.children.get(0)));//order is important here
                        childrenHere.addAll(arg2Children.get((DottedCommandParser.ArgumentContext) ctx.children.get(2)));//order is important here
                        arg2Children.put(ctx, childrenHere);
                    } else {
                        arg2Children.put(ctx, Collections.singletonList(parses.get(ctx.children.get(0))));
                    }
                }

            };

            walker.walk(listener, tree);
            ret.add(parses.get(tree));
        }

        return ret;
    }

    /**
     * Takes a tree of commands (commands are still strings) and turns them into
     * operations.
     *
     * @param codeTree
     * @param variableTypeTracker
     * @return
     */
    private Tree<Operation> getOperationsFromCodeTree(Tree<String> codeTree, Class[] variableTypeTracker, boolean lineIsGlobal) {
        Map<String, Method> allAnnotatedStaticMethods = getAllAnnotatedStaticMethods();
        Map<String, Constructor> allAnnotatedConstructors = getAllAnnotatedConstructors();
        return codeTree.dfs((Tree<String> node, List<Tree<Operation>> childrenValues) -> {
            String code = node.getLabel();
            Operation retLabel;
            switch (code) {
                case NULL_SYMBOL:
                    retLabel = new Operation.NullOperation();
                    break;
                case BASE_IRTG_SYMBOL:
                    retLabel = new Operation.LookupVariableOperation(globalVariableTracker, variableTypeTracker, 0, "global");//no need to modify neededForGlobal, since this is global itself
                    break;
                default:
                    if (code.startsWith(ADDITIONAL_DATA_SYMBOL)) {
                        try {
                            int additionalDataNr = Integer.valueOf(code.substring(1));
                            if (additionalDataNr > additionalData.size()) {
                                System.err.println("WARNING: Additional data number " + code.substring(1) + " was not given. Using null instead");
                                retLabel = new Operation.NullOperation();
                            } else {
                                retLabel = new Operation.LookupVariableOperation(globalVariableTracker, variableTypeTracker, shiftAdditionalDataIndex(additionalDataNr), "global");//no modification of neededForGlobal necessary, since this is global already
                            }
                        } catch (NumberFormatException ex) {
                            System.err.println("Error parsing '" + code + "', expected number after '" + ADDITIONAL_DATA_SYMBOL + "' to indicate additional data index");
                            retLabel = new Operation.NullOperation();
                        }
                    } else if (code.startsWith(LEFT_INPUT_DELIMITER) && code.endsWith(RIGHT_INPUT_DELIMITER)) {
                        String interpName = code.substring(LEFT_INPUT_DELIMITER.length(), code.length() - RIGHT_INPUT_DELIMITER.length());

                        if (firstMentionedInterpretation == null) {
                            firstMentionedInterpretation = interpName;
                        }

                        int lookupIndex = interp2Index.getInt(interpName);

                        if (lineIsGlobal) {
                            neededForGlobal[lookupIndex] = true;
                        }

                        retLabel = new Operation.LookupVariableOperation(variableTracker, variableTypeTracker, lookupIndex, null);
                    } else if (code.startsWith(LEFT_STRING_DELIMITER) && code.endsWith(RIGHT_STRING_DELIMITER)) {
                        retLabel = new Operation.StringOperation(code.substring(LEFT_STRING_DELIMITER.length(), code.length() - RIGHT_STRING_DELIMITER.length()));
                    } else {
                        try {
                            retLabel = new Operation.PrimitiveTypeOperation(code);
                        } catch (NumberFormatException ex) {
                            if (childrenValues.isEmpty()) {
                                //then we have a variable or empty constructor / static method
                                if (allAnnotatedConstructors.containsKey(code)) {
                                    retLabel = new Operation.ConstructorOperation(allAnnotatedConstructors.get(code));
                                } else if (allAnnotatedStaticMethods.containsKey(code)) {
                                    retLabel = new Operation.MethodOperation(allAnnotatedStaticMethods.get(code), true);
                                } else {
                                    try {
                                        int lookupIndex = shiftIndex(varName2Index.get(code));
                                        if (lineIsGlobal) {
                                            retLabel = new Operation.LookupVariableOperation(globalVariableTracker, variableTypeTracker, lookupIndex, "global");//no need to modify neededForGlobal here, since this is global itself
                                            neededForGlobal[lookupIndex] = true;
                                        } else if (isGlobal[lookupIndex]) {
                                            retLabel = new Operation.LookupVariableOperation(globalVariableTracker, variableTypeTracker, lookupIndex, "global");
                                        } else {
                                            retLabel = new Operation.LookupVariableOperation(variableTracker, variableTypeTracker, lookupIndex, null);
                                        }
                                    } catch (java.lang.Exception ex1) {
                                        System.err.println("Could not handle code: Code = " + code);
                                        System.err.println(interp2Index);
                                        throw new RuntimeException("Could not handle code: Code = " + code);//TODO: make an exception for this
                                    }
                                }
                            } else if (allAnnotatedConstructors.containsKey(code)) {
                                retLabel = new Operation.ConstructorOperation(allAnnotatedConstructors.get(code));
                            } else if (allAnnotatedStaticMethods.containsKey(code)) {
                                retLabel = new Operation.MethodOperation(allAnnotatedStaticMethods.get(code), true);
                            } else {
                                //the we have a non-static method

                                //get type from leftmost child
                                Class invokingType = childrenValues.get(0).getLabel().getReturnType();
                                Method m = null;
                                for (Method candidate : invokingType.getMethods()) {
                                    OperationAnnotation annot = findAnnotation(candidate, OperationAnnotation.class);
                                    //TODO: write testcase that checks for duplicate codes in one class or its superclasses
                                    if (annot != null) {
                                        //System.err.println("--- CODE FOUND ----- :: "+annot.code());
                                        if (annot.code().equals(code)) {
                                            m = candidate;
                                        }
                                    }
                                }
                                if (m == null) {
                                    throw new RuntimeException(new ParseException("Code " + code + " not found in class " + invokingType.getName()));
                                }
                                if (m.equals(GET_ALG_METHOD)) {
                                    String interpName;
                                    try {
                                        interpName = (String) childrenValues.get(0).getChildren().get(1).getLabel().apply(null); //this is kinda hacky
                                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException ex1) {
                                        throw new RuntimeException(new ParseException("Bad error in code when looking up Interpretation#getAlgebra return type: " + ex1.toString()));
                                    }
                                    Class algClass = ((InterpretedTreeAutomaton) globalVariableTracker[0]).getInterpretation(interpName).getAlgebra().getClass();//this is kinda hacky as well
                                    retLabel = new Operation.MethodOperation(m, algClass, false);
                                } else {
                                    retLabel = new Operation.MethodOperation(m, false);
                                }
                            }

                        }
                    }
            }
            return Tree.create(retLabel, childrenValues);
        });
    }

    //The following method is copied and adapted from org.springframework.core.annotation.AnnotationUtils.findAnnotation(Method, Class),
    //distributed under the Apache License 2.0. See the included file APACHE-LICENSE-2.0.txt.
    static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        A result = method.getAnnotation(annotationType);
        Class<?> clazz = method.getDeclaringClass();
        while (result == null) {
            clazz = clazz.getSuperclass();
            if (clazz == null || Object.class == clazz) {
                break;
            }
            try {
                Method equivalentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                result = equivalentMethod.getAnnotation(annotationType);
            } catch (NoSuchMethodException ex) {
                // No equivalent method found
            }
        }
        return result;
    }

    private InterpretedTreeAutomaton getGlobalIrtg() {
        return (InterpretedTreeAutomaton) globalVariableTracker[0];
    }

    private void parseProgram(List<Tree<String>> unparsedProgram, InterpretedTreeAutomaton baseIRTG) {
        Class[] variableTypeTracker = new Class[shiftIndex(unparsedProgram.size())];

        //assigning the baseIRTG to the variableTracker
        globalVariableTracker[0] = baseIRTG;
        variableTypeTracker[0] = baseIRTG.getClass();
        Iterator<String> interpsIterator = baseIRTG.getInterpretations().keySet().iterator();
        for (int i = 0; i < interpCount; i++) {
            String interpName = interpsIterator.next();
            try {
                variableTypeTracker[1 + i] = baseIRTG.getInterpretation(interpName).getAlgebra().getClass().getMethod("parseString", new Class[]{String.class}).getReturnType();//this is kinda hacky
            } catch (NoSuchMethodException | SecurityException ex) {
                System.err.println("Error finding input " + interpName + " class!");
            }
            interp2Index.put(interpName, 1 + i);
        }

        for (int i = 0; i < additionalData.size(); i++) {
            globalVariableTracker[shiftAdditionalDataIndex(i)] = additionalData.get(i);
        }

//        for( int i = 1; i < globalVariableTracker.length; i++ ) {
//            Object x = globalVariableTracker[i];
//            System.err.printf("%d: %s", i, x == null ? "<null>" : x.toString());
//        }
        
        for (int i = 0; i < unparsedProgram.size(); i++) {
            Tree<String> command = unparsedProgram.get(i);
            Tree<Operation> parsedCommand = getOperationsFromCodeTree(command, variableTypeTracker, isGlobal[shiftIndex(i)]);
            Class returnType = parsedCommand.getLabel().getReturnType();
            variableTypeTracker[shiftIndex(i)] = returnType;
            isNumeric[shiftIndex(i)] = isClassNumeric(returnType);
            program.add(parsedCommand);
        }

    }

    @SuppressWarnings("ThrowableResultIgnored")
    private Int2ObjectMap<Throwable> run(int instanceId, Instance instance, Tree<Operation>[] localProgram, Object[] localVariableTracker, Map<String, CpuTimeStopwatch> name2Watch) {

        for (String interpName : interp2Index.keySet()) {
            localVariableTracker[interp2Index.get(interpName)] = instance.getInputObjects().get(interpName);
        }

        Int2ObjectMap<Throwable> ret = new Int2ObjectOpenHashMap<>();

        //run, and measure times --TODO: runtimes
        for (int i = 0; i < localProgram.length; i++) {

            //not sure if this is too much overhead, but it should be a few orders of magnitude below 1 ms
            List<String> watchesStartingHere = watchesStartingBefore.get(i);
            if (watchesStartingHere != null) {
                for (String watchName : watchesStartingHere) {
                    name2Watch.get(watchName).record(0);
                }
            }

            if (!isGlobal[shiftIndex(i)]) {
                Object res = null;
                try {
                    res = Operation.executeTree(localProgram[i]);
                } catch (Throwable error) {
                    ret.put(i, error);//the ThrowableResultIgnored warning does not apply here, so I suppressed it
                }
                //String resString = (res == null) ? "null" : res.toString();
                //System.err.println(resString.substring(0, Math.min(30, resString.length())));
                localVariableTracker[shiftIndex(i)] = res;
            }
            List<String> watchesStoppingHere = watchesStoppingAfter.get(i);
            if (watchesStoppingHere != null) {
                for (String watchName : watchesStoppingHere) {
                    name2Watch.get(watchName).record(1);
                }
            }
        }

        // for testing
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, null, ex);
//        }
        return ret;
    }

    @SuppressWarnings("ThrowableResultIgnored")
    private Int2ObjectMap<Throwable> runGlobal(boolean init) {
        Int2ObjectMap<Throwable> ret = new Int2ObjectOpenHashMap<>();
        //run, and measure times --TODO: runtimes
        for (int i = 0; i < program.size(); i++) {
            if (isGlobal[shiftIndex(i)] && (isInitialization[shiftIndex(i)] == init)) {

                Object res = null;
                try {
                    res = Operation.executeTree(program.get(i));
                } catch (Throwable error) {
                    ret.put(i, error);//the ThrowableResultIgnored warning does not apply here, so I suppressed it
                }
                //String resString = (res == null) ? "null" : res.toString();
                //System.err.println(resString.substring(0, Math.min(30, resString.length())));
                globalVariableTracker[shiftIndex(i)] = res;
            }
        }

        return ret;
    }

    private static Tree<Operation> createLocalOperationTreeCopy(Tree<Operation> original, Object[] variableTracker) {
        return original.dfs((Tree<Operation> node, List<Tree<Operation>> childrenValues) -> {
            Operation newOp = node.getLabel();

            //replace lookups to local variableTracker
            if (newOp instanceof Operation.LookupVariableOperation) {
                Operation.LookupVariableOperation lookupOp = (Operation.LookupVariableOperation) newOp;

                //only have to replace if lookup is not global (operations for all instances use same global lookup array)
                if (lookupOp.getExtra() == null || !lookupOp.getExtra().equals("global")) {
                    newOp = new Operation.LookupVariableOperation(variableTracker, lookupOp.getResultsTypeTracker(), lookupOp.getIndex(), lookupOp.getExtra());
                }
            }
            return Tree.create(newOp, childrenValues);
        });
    }

    /**
     * Runs the program on all instances in the corpus.
     *
     * @param corpus
     * @param resMan Determines what happens with the results
     * @param onInstanceSuccess What to do once an instance is done (for example
     * giving the user feedback).
     * @param maxInstances Cancels the run when this number of instances is
     * reached. input -1 to parse the whole corpus.
     */
    public void run(Corpus corpus, ResultManager resMan, IntConsumer onInstanceSuccess, int maxInstances, boolean isWarmup, Set<String> verboseMeasurements) {
        boolean verbose = (verboseMeasurements != null);
        boolean verboseAll = (verboseMeasurements != null) && verboseMeasurements.contains(VERBOSE_ALL);
        int width = (int) (Math.ceil(Math.log10(corpus.getNumberOfInstances())));
        String formatString = verbose ? "%0" + width + "d [%-50.50s] " : null;
        Algebra firstAlgebra = verbose ? getGlobalIrtg().getInterpretation(firstMentionedInterpretation).getAlgebra() : null;

        // brute force make everything explicit for parallelisation. TODO: do this properly
        // force building the bottom-up rule index
        ((InterpretedTreeAutomaton) globalVariableTracker[0]).getAutomaton().getRuleSet();
        if (corpus.iterator().hasNext()) {
            Instance testInstance = corpus.iterator().next();
            for (Entry<String, Object> entry : testInstance.getInputObjects().entrySet()) {
                ((InterpretedTreeAutomaton) globalVariableTracker[0]).filterBinarizedForAppearingConstants(entry.getKey(), entry.getValue());
                ((InterpretedTreeAutomaton) globalVariableTracker[0]).filterForAppearingConstants(entry.getKey(), entry.getValue());
            }
        }

        //first setup the localResults array
        for (int j = 0; j < globalVariableTracker.length; j++) {
            if (!isGlobal[j]) {
                globalVariableTracker[j] = new Object[corpus.getNumberOfInstances()];
            }
        }

        //keep track of measured times globally
        Map<String, long[]> watchName2Times = new HashMap<>();
        for (String watchName : allWatchNames) {
            watchName2Times.put(watchName, new long[corpus.getNumberOfInstances()]);
        }

        //run initialization
        Int2ObjectMap<Throwable> initErrors = runGlobal(true);

        //accept init data in ResultManager
        for (int k = 0; k < program.size(); k++) {
            if (isGlobal[shiftIndex(k)] && isInitialization[shiftIndex(k)]) {
                resMan.acceptResult(globalVariableTracker[shiftIndex(k)], -1, varNames[k], doExport[shiftIndex(k)], true, isNumeric[shiftIndex(k)]);
            }
        }
        //TODO: accept runtimes -- EDIT: currently only measuring non-global runtimes

        for (Int2ObjectMap.Entry<Throwable> idAndError : initErrors.int2ObjectEntrySet()) {
            int k = idAndError.getIntKey();
            //do not need to check if entry for k is global, only global k can appear here.
            resMan.acceptError(idAndError.getValue(), -1, varNames[k], doExport[shiftIndex(k)], true);
        }

        // Now iterate over the instances in the corpus. One Runnable is offered to the
        // fork-join pool for each instance right away. This means that the entire corpus
        // is loaded immediately (even if any data structures for parsing are not allocated
        // before each actual computation starts). If the corpora ever grow to a size that
        // makes this infeasible in terms of memory, we should look into implementing a
        // spliterator for Corpus that supports parallelization in a more fine-grained way.
        MutableInteger instanceID = new MutableInteger(0);         // assigns unique ID to instance when processing starts
        MutableInteger instanceCounter = new MutableInteger(1);    // counts up instance number when processing finishes, for updating the progress bar
        ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);

        for (Instance instance : corpus) {
            forkJoinPool.execute(() -> {
                // get unique instance-counter value i for this instance
                int i = -1;
                synchronized (instanceID) {
                    i = instanceID.incValue();
                }

//                System.err.println("exec: " + i);
                //make local copies of global prototypical objects
                Object[] variableTrackerHere = new Object[variableTracker.length];
                Tree<Operation>[] localProgram = new Tree[program.size()];
                for (int j = 0; j < program.size(); j++) {
                    localProgram[j] = createLocalOperationTreeCopy(program.get(j), variableTrackerHere);
                }

                if (i >= maxInstances && maxInstances > -1) {
                    return;
                }

                Map<String, CpuTimeStopwatch> name2Watch = new HashMap<>();
                for (List<String> watchNameList : watchesStartingBefore.values()) {
                    if (watchNameList != null) {
                        for (String watchName : watchNameList) {
                            name2Watch.put(watchName, new CpuTimeStopwatch());
                        }
                    }
                }

                // process one instance
                if (verbose) {
                    System.err.printf(formatString, i, firstAlgebra.representAsString(instance.getInputObjects().get(firstMentionedInterpretation)));
                }

                Int2ObjectMap<Throwable> errors = run(i, instance, localProgram, variableTrackerHere, name2Watch);

                //now accept data in ResultManager
                for (int k = 0; k < program.size(); k++) {
                    //use this loop instead of just iterating over watch2Name.entrySet() in order to get only watches that were stopped.
                    List<String> watchesStoppingHere = watchesStoppingAfter.get(k);
                    if (watchesStoppingHere != null) {
                        for (String watchName : watchesStoppingHere) {
                            CpuTimeStopwatch watch = name2Watch.get(watchName);
                            long time = watch.getTimeBefore(1) / 1000000;//want time in ms
                            resMan.acceptTime(time, i, watchName, false);
                            watchName2Times.get(watchName)[i] = time;

                            if (verbose) {
                                System.err.printf("%s:%s ", watchName, Util.formatTime(watch.getTimeBefore(1)));
                            }
                        }
                    }

                    if (!isGlobal[shiftIndex(k)]) {
                        Object value = variableTrackerHere[shiftIndex(k)];
                        resMan.acceptResult(value, i, varNames[k], doExport[shiftIndex(k)], false, isNumeric[shiftIndex(k)]);

                        if (verbose) {
                            if (verboseAll || verboseMeasurements.contains(varNames[k])) {
                                if (doExport[shiftIndex(k)]) {
                                    if (value == null) {
                                        System.err.printf("%s=<null> ", varNames[k]);
                                    } else {
                                        System.err.printf("%s=%s ", varNames[k], value.toString());
                                    }
                                }
                            }

                        }
                    }
                }

                if (verbose) {
                    System.err.println();
                }

                for (Int2ObjectMap.Entry<Throwable> idAndError : errors.int2ObjectEntrySet()) {
                    int k = idAndError.getIntKey();
                    //do not need to check if entry for k is not global, only non-global k can appear here.
                    resMan.acceptError(idAndError.getValue(), i, varNames[k], doExport[shiftIndex(k)], false);
                }

                for (int j = 0; j < globalVariableTracker.length; j++) {
                    if (!isGlobal[j]) {
                        if (neededForGlobal[j]) {
                            ((Object[]) globalVariableTracker[j])[i] = variableTrackerHere[j];
                        }
                    }
                }

                if (onInstanceSuccess != null) {
                    synchronized (instanceCounter) {
                        onInstanceSuccess.accept(instanceCounter.incValue());
                    }
                }
            });
        }

        forkJoinPool.shutdown();
        try {
            forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!isWarmup) {      // only report global results if not warmup
            //run globally
            Int2ObjectMap<Throwable> errors = runGlobal(false);
        //System.err.println("--------------------global--------------------");
            //System.err.println(Arrays.toString(globalVariableTracker));

            //accept global data in ResultManager
            for (int k = 0; k < program.size(); k++) {
                if (isGlobal[shiftIndex(k)] && !isInitialization[shiftIndex(k)]) {
                    resMan.acceptResult(globalVariableTracker[shiftIndex(k)], -1, varNames[k], doExport[shiftIndex(k)], true, isNumeric[shiftIndex(k)]);

                    if (verbose && doExport[shiftIndex(k)]) {
                        Object val = globalVariableTracker[shiftIndex(k)];

                        if (val == null) {
                            System.err.printf("%s: <null>\n", varNames[k]);
                        } else {
                            System.err.printf("%s: %s\n", varNames[k], val.toString());
                        }
                    }
                }
            }

            //upload time sums
            for (Entry<String, long[]> nameAndTime : watchName2Times.entrySet()) {
                long sum;
                try {
                    sum = Arrays.stream(nameAndTime.getValue()).reduce((long left, long right) -> left + right).getAsLong();
                } catch (java.util.NoSuchElementException ex) {
                    sum = 0;
                }

                resMan.acceptTime(sum, -1, "total " + nameAndTime.getKey(), true);
                System.err.printf("total %s: %s\n", nameAndTime.getKey(), Util.formatTime(sum * 1000000L)); // convert back to ns
            }

            //TODO: accept runtimes -- EDIT: currently only measuring non-global runtimes
            for (Int2ObjectMap.Entry<Throwable> idAndError : errors.int2ObjectEntrySet()) {
                int k = idAndError.getIntKey();
                //do not need to check if entry for k is global, only global k can appear here.
                resMan.acceptError(idAndError.getValue(), -1, varNames[k], doExport[shiftIndex(k)], true);
            }
        }
    }

    private boolean isClassNumeric(Class maybeNumber) {
        return (maybeNumber == Double.class || maybeNumber == Float.class || maybeNumber == Long.class
                || maybeNumber == Integer.class || maybeNumber == Short.class || maybeNumber == Byte.class
                || maybeNumber == double.class || maybeNumber == float.class || maybeNumber == long.class
                || maybeNumber == int.class || maybeNumber == short.class || maybeNumber == byte.class);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, ParseException, Throwable {

        //System.out.println(getAllAnnotatedConstructors());
        Map<String, Method> ms = getAllAnnotatedStaticMethods();
        String longest = Collections.max(ms.keySet(), Comparator.comparing(s -> s.length()));
        int maxLen = longest.length();

        for (String key : ms.keySet()) {
            System.err.printf("[%-" + maxLen + "s] %s\n", key, ms.get(key).toString());
        }

        System.exit(0);

//        List<String> programCode = new ArrayList<>();
//        programCode.add("export pruningPolicy = histogramPP(insideFOM, 10)");
//        /*programCode.add("starttime all");
//         programCode.add("starttime filter");
//         programCode.add("F = filter($, __graph__, <graph>)");
//         programCode.add("stoptime filter");
//         programCode.add("D = F.[graph].alg.decomp(<graph>)");
//         programCode.add("Invhom = F.[graph].invhom(D)");
//         programCode.add("Intersect = F.auto.intersect(Invhom)");
//         programCode.add("export Vit = Intersect.viterbi");
//         programCode.add("Tree = F.[graph].hom.apply(Vit)");
//         programCode.add("starttime eval");
//         programCode.add("export Result = F.[graph].alg.eval(Tree)");
//         programCode.add("stoptime eval");
//         programCode.add("stoptime all");*/
//        /*for (Tree<String> tree : program) {
//         System.err.println(tree);
//         }*/
//
//        InterpretedTreeAutomaton irtg = InterpretedTreeAutomaton.read(new FileInputStream("examples/hrg.irtg"));
//
//        /*List<Tree<String>> program = new ArrayList<>();
//         program.add(TreeParser.parse("filter('$', '__graph__', '<graph>')"));
//         program.add(TreeParser.parse("decomp(alg(interp(F,'__graph__')),'<graph>')"));
//         program.add(TreeParser.parse("invhom(interp(F,'__graph__'),D)"));
//         program.add(TreeParser.parse("intersect(auto(F),Invhom)"));
//         program.add(TreeParser.parse("viterbi(Intersect)"));
//         program.add(TreeParser.parse("apply(hom(interp(F,'__string__')), Vit)"));
//         program.add(TreeParser.parse("eval(alg(interp(F,'__string__')), Tree)"));*/
//        Program prog = new Program(irtg, null, programCode, new HashMap<>());
//
//        Instance instance = new Instance();
//        Map<String, Object> input = new HashMap<>();
//        input.put("string", new GraphAlgebra().parseString("(w / want  :ARG0 (b / boy)  :ARG1 (g / go :ARG0 b))"));
//        instance.setInputObjects(input);
//        Corpus testCorpus = new Corpus();
//        testCorpus.addInstance(instance);
//        prog.run(testCorpus, new ResultManager.PrintingManager(), null, -1);
//        /*for (String varName : prog.varName2Index.keySet()) {
//         String resString = prog.getLastResults()[prog.varName2Index.get(varName)].toString();
//         System.err.println();
//         System.err.println(varName+" = "+resString.substring(0, Math.min(30, resString.length())));
//         System.err.println();
//         }*/
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

}
