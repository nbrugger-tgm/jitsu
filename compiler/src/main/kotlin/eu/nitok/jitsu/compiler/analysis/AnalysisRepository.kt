package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.elements.VariableReference

internal class AnalysisRepository {
    private val functionSummaries: MutableMap<FunctionElement, FunctionSummaryElement> = mutableMapOf()
    private val useSiteInfos: MutableMap<VariableReference, UseSiteInfo> = mutableMapOf()

    fun analyzeAll(functions: Collection<FunctionElement>, messages: CompilerMessages) {
        val allFunctions = collectAllFunctions(functions)
        val callGraph = buildCallGraph(allFunctions)
        val sccs = computeSCCs(callGraph)
        for (scc in sccs) {
            solveSCC(scc, callGraph, messages)
        }
    }

    fun getFunctionSummary(function: FunctionElement): FunctionSummaryElement? = functionSummaries[function]
    fun getUseSiteInfo(ref: VariableReference): UseSiteInfo? = useSiteInfos[ref]

    /**
     * Flat maps all functions and their nested functions into a list
     */
    private fun collectAllFunctions(rootFunctions: Collection<FunctionElement>): List<FunctionElement> {
        val all = mutableListOf<FunctionElement>()
        val seen = mutableSetOf<FunctionElement>()
        for (root in rootFunctions) {
            root.sequence().forEach { element ->
                if (element is FunctionElement && seen.add(element)) {
                    all.add(element)
                }
            }
        }
        return all
    }

    private fun buildCallGraph(functions: List<FunctionElement>): Map<FunctionElement, Set<FunctionElement>> {
        val functionSet = functions.toSet()
        val functionsByName = functions.filter { it.name != null }.groupBy { it.name!!.value }
        return functions.associateWith { fn ->
            fn.accessFromSelf
                .filterIsInstance<Access.FunctionAccess>()
                .flatMap { access ->
                        //This stinks since it puts all methods with the matching name into the list,
                        // but that also includes wrong overloads (non matching param types)
                        functionsByName[access.reference.value] ?: emptyList()
                }
                .filter { it in functionSet }
                .toSet()
        }
    }

    private fun computeSCCs(callGraph: Map<FunctionElement, Set<FunctionElement>>): List<Set<FunctionElement>> {
        val index = mutableMapOf<FunctionElement, Int>()
        val lowlink = mutableMapOf<FunctionElement, Int>()
        val onStack = mutableSetOf<FunctionElement>()
        val stack = mutableListOf<FunctionElement>()
        val sccs = mutableListOf<Set<FunctionElement>>()
        var indexCounter = 0

        fun strongConnect(v: FunctionElement) {
            index[v] = indexCounter
            lowlink[v] = indexCounter
            indexCounter++
            stack.add(v)
            onStack.add(v)

            val successors = callGraph[v] ?: emptySet()
            for (w in successors) {
                if (!index.containsKey(w)) {
                    strongConnect(w)
                    lowlink[v] = minOf(lowlink[v]!!, lowlink[w]!!)
                } else if (onStack.contains(w)) {
                    lowlink[v] = minOf(lowlink[v]!!, index[w]!!)
                }
            }

            if (lowlink[v] == index[v]) {
                val scc = mutableSetOf<FunctionElement>()
                while (true) {
                    val w = stack.removeLast()
                    onStack.remove(w)
                    scc.add(w)
                    if (w == v) break
                }
                sccs.add(scc)
            }
        }

        for (fn in callGraph.keys) {
            if (!index.containsKey(fn)) {
                strongConnect(fn)
            }
        }

        return sccs
    }

    private fun solveSCC(scc: Set<FunctionElement>, callGraph: Map<FunctionElement, Set<FunctionElement>>, messages: CompilerMessages) {
        val isSelfRecursive = scc.size == 1 && callGraph[scc.first()]?.contains(scc.first()) == true

        if (scc.size == 1 && !isSelfRecursive) {
            val fn = scc.first()
            val analyzer = CodeBlockAnalyzer(fn, this::getFunctionSummary, messages)
            val result = analyzer.analyze()
            functionSummaries[fn] = result.functionSummary
            useSiteInfos.putAll(result.useSiteInfos)
        } else {
            for (fn in scc) {
                functionSummaries[fn] = FunctionSummaryElement.optimistic(fn.parameters.map { it.name.value })
            }

            val worklist = scc.toMutableSet()
            val maxIterations = 100
            var iterations = 0

            while (worklist.isNotEmpty() && iterations < maxIterations) {
                iterations++
                val fn = worklist.first()
                worklist.remove(fn)

                val oldSummary = functionSummaries[fn]!!
                val iterMessages = CompilerMessages()
                val analyzer = CodeBlockAnalyzer(fn, { getFunctionSummary(it) }, iterMessages)
                val result = analyzer.analyze()

                if (!result.functionSummary.structurallyEquals(oldSummary)) {
                    functionSummaries[fn] = result.functionSummary
                    val inSCCCallers = scc.filter { caller ->
                        callGraph[caller]?.contains(fn) == true && caller != fn
                    }
                    worklist.addAll(inSCCCallers)
                    if (callGraph[fn]?.contains(fn) == true) {
                        worklist.add(fn)
                    }
                }

                useSiteInfos.putAll(result.useSiteInfos)
            }

            for (fn in scc) {
                val analyzer = CodeBlockAnalyzer(fn, { getFunctionSummary(it) }, messages)
                val result = analyzer.analyze()
                functionSummaries[fn] = result.functionSummary
                useSiteInfos.putAll(result.useSiteInfos)
            }
        }
    }
}
