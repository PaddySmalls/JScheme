package hdm.pk070.jscheme.eval;

import hdm.pk070.jscheme.error.SchemeError;
import hdm.pk070.jscheme.obj.SchemeObject;
import hdm.pk070.jscheme.obj.builtin.function.SchemeBuiltinFunction;
import hdm.pk070.jscheme.obj.builtin.simple.SchemeCons;
import hdm.pk070.jscheme.obj.builtin.simple.SchemeNil;
import hdm.pk070.jscheme.obj.builtin.simple.SchemeSymbol;
import hdm.pk070.jscheme.obj.builtin.syntax.SchemeBuiltinSyntax;
import hdm.pk070.jscheme.reader.SchemeReader;
import hdm.pk070.jscheme.stack.SchemeCallStack;
import hdm.pk070.jscheme.table.environment.Environment;
import hdm.pk070.jscheme.table.environment.entry.EnvironmentEntry;

/**
 * A class for evaluating all kinds of Scheme lists.
 *
 * @author patrick.kleindienst
 */
class ListEvaluator extends AbstractEvaluator<SchemeCons> {

    static ListEvaluator getInstance() {
        return new ListEvaluator();
    }

    private ListEvaluator() {
    }

    /**
     * Takes an arbitrary list expression from the {@link SchemeReader} as well as an {@link Environment} and passes
     * it along to a specified evaluation class depending on what kind of expression it has been given. For this, it
     * has to evaluate the function slot, which is accessible as the CAR of the list expression.
     *
     * @param expression
     *         The list expression to evaluate
     * @param environment
     *         The evaluation context
     * @return A {@link SchemeObject} as a list evaluation result
     * @throws SchemeError
     */
    @Override
    public SchemeObject doEval(SchemeCons expression, Environment<SchemeSymbol, EnvironmentEntry> environment) throws
            SchemeError {
        // Extract function slot (car of expression)
        SchemeObject functionSlot = expression.getCar();
        // Evaluate function slot
        SchemeObject evaluatedFunctionSlot = SchemeEval.getInstance().eval(functionSlot, environment);
        // Extract arg list (cdr of expression)
        SchemeObject argumentList = expression.getCdr();

        // Check if function slot is built-in function
        if (evaluatedFunctionSlot.subtypeOf(SchemeBuiltinFunction.class)) {
            return evaluateBuiltinFunction(((SchemeBuiltinFunction) evaluatedFunctionSlot), argumentList, environment);

            // Check if function slot is built-in syntax (e.g. define)
        } else if (evaluatedFunctionSlot.subtypeOf(SchemeBuiltinSyntax.class)) {
            return evaluateBuiltinSyntax(((SchemeBuiltinSyntax) evaluatedFunctionSlot), argumentList, environment);
        }

        // Reaching this section means we don't have a valid function slot -> throw SchemeError
        throw new SchemeError(String.format("application: not a procedure [expected: procedure that can be applied to" +
                " arguments, given: %s]", functionSlot));
    }

    private SchemeObject evaluateBuiltinSyntax(SchemeBuiltinSyntax builtinSyntax, SchemeObject argumentList,
                                               Environment<SchemeSymbol, EnvironmentEntry> environment) throws
            SchemeError {

        return builtinSyntax.apply(argumentList, environment);
    }

    /**
     * Gets passed a {@link SchemeBuiltinFunction} and evaluates it in the context of the {@link Environment}
     * argument.
     *
     * @param builtinFunction
     *         The pre-defined function to call
     * @param argumentList
     *         The argument list for the function call
     * @param environment
     *         The {@link Environment} the function body gets evaluated in
     * @return A {@link SchemeObject} as a result of the function call
     * @throws SchemeError
     */
    private SchemeObject evaluateBuiltinFunction(SchemeBuiltinFunction builtinFunction, SchemeObject argumentList,
                                                 Environment<SchemeSymbol, EnvironmentEntry> environment) throws
            SchemeError {
        SchemeObject restArguments;
        restArguments = argumentList;
        int argumentCount = 0;

        // as long as end of argumentList is not reached ...
        while (!restArguments.typeOf(SchemeNil.class)) {
            SchemeObject currentArgument = ((SchemeCons) restArguments).getCar();
            SchemeObject evaluatedArgument = SchemeEval.getInstance().eval(currentArgument, environment);
            restArguments = ((SchemeCons) restArguments).getCdr();

            // push evaluated arg to stack
            SchemeCallStack.instance().push(evaluatedArgument);

            // increment arg counter
            argumentCount++;
        }

        // call built-in function
        return builtinFunction.call(argumentCount);
    }
}
