/**
 *
 */
package metaheuristics.grasp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import problems.Evaluator;
import solutions.Solution;

/**
 * Abstract class for metaheuristic GRASP (Greedy Randomized Adaptive Search
 * Procedure). It consider a minimization problem.
 *
 * @param <E> Generic type of the element which composes the solution.
 * @author ccavellucci, fusberti
 */
public abstract class AbstractGRASP<E> {

    /**
     * flag that indicates whether the code should print more information on
     * screen
     */
    public static boolean verbose = true;

    /**
     * a random number generator
     */
    static Random rng = new Random(0);

    /**
     * the objective function being optimized
     */
    protected Evaluator<E> ObjFunction;

    /**
     * the GRASP greediness-randomness parameter
     */
    protected Double alpha;

    /**
     * the best (incumbent) solution cost
     */
    protected Double bestCost;

    /**
     * the current solution cost
     */
    protected Double cost;

    /**
     * the best solution
     */
    protected Solution<E> bestSol;

    /**
     * the current solution
     */
    protected Solution<E> sol;

    /**
     * the number of iterations the GRASP main loop executes.
     */
    protected Integer iterations;

    /**
     * the Candidate List of elements to enter the solution.
     */
    protected ArrayList<E> CL;

    /**
     * the Restricted Candidate List of elements to enter the solution.
     */
    protected ArrayList<E> RCL;

    /**
     * Creates the Candidate List, which is an ArrayList of candidate elements
     * that can enter a solution.
     *
     * @return The Candidate List.
     */
    public abstract ArrayList<E> makeCL();

    /**
     * Creates the Restricted Candidate List, which is an ArrayList of the best
     * candidate elements that can enter a solution. The best candidates are
     * defined through a quality threshold, delimited by the GRASP
     * {@link #alpha} greedyness-randomness parameter.
     *
     * @return The Restricted Candidate List.
     */
    public abstract ArrayList<E> makeRCL();

    /**
     * Updates the Candidate List according to the current solution
     * {@link #sol}. In other words, this method is responsible for
     * updating which elements are still viable to take part into the solution.
     */
    public abstract void updateCL();

    /**
     * Creates a new solution which is empty, i.e., does not contain any
     * element.
     *
     * @return An empty solution.
     */
    public abstract Solution<E> createEmptySol();

    /**
     * The GRASP local search phase is responsible for repeatedly applying a
     * neighborhood operation while the solution is getting improved, i.e.,
     * until a local optimum is attained.
     *
     * @return An local optimum solution.
     */
    public abstract Solution<E> localSearch();

    /**
     * Creates an Evaluator based on the parmeters in the input file.
     *
     * @return The created evaluator.
     */
    protected abstract Evaluator<E> initEvaluator(String filename) throws IOException;

    /**
     * Constructor for the AbstractGRASP class.
     *
     * @param filename   The file containing the objective function parameters.
     * @param alpha      The GRASP greediness-randomness parameter (within the range
     *                   [0,1])
     * @param iterations The number of iterations which the GRASP will be executed.
     */
    public AbstractGRASP(String filename, Double alpha, Integer iterations) throws IOException {
        this.ObjFunction = initEvaluator(filename);
        this.alpha = alpha;
        this.iterations = iterations;
    }

    /**
     * The GRASP constructive heuristic, which is responsible for building a
     * feasible solution by selecting in a greedy-random fashion, candidate
     * elements to enter the solution.
     *
     * @return A feasible solution to the problem being minimized.
     */
    public Solution<E> constructiveHeuristic() {
        CL = makeCL();
        RCL = makeRCL();
        sol = createEmptySol();
        cost = Double.POSITIVE_INFINITY;

        /* Main loop, which repeats until the stopping criteria is reached. */
        while (!constructiveStopCriteria()) {
            double maxCost = Double.NEGATIVE_INFINITY, minCost = Double.POSITIVE_INFINITY;
            cost = ObjFunction.evaluate(sol);
            updateCL();
            Double[] deltas = new Double[CL.size()];

            /*
             * Explore all candidate elements to enter the solution, saving the
             * highest and lowest cost variation achieved by the candidates.
             */
            for (int i = 0; i < CL.size(); i++) {
                Double deltaCost = ObjFunction.evaluateInsertionCost(CL.get(i), sol);
                deltas[i] = deltaCost;
                if (deltaCost < minCost)
                    minCost = deltaCost;
                if (deltaCost > maxCost)
                    maxCost = deltaCost;
            }

            /*
             * Among all candidates, insert into the RCL those with the highest
             * performance using parameter alpha as threshold.
             */
            for (int i = 0; i < CL.size(); i++) {
                Double deltaCost = deltas[i];
                if (deltaCost <= minCost + alpha * (maxCost - minCost))
                    RCL.add(CL.get(i));
            }

            /* Choose a candidate randomly from the RCL */
            int rndIndex = rng.nextInt(RCL.size());
            E inCand = RCL.get(rndIndex);
            CL.remove(inCand);
            sol.add(inCand);
            ObjFunction.evaluate(sol);
            RCL.clear();
        }

        return sol;
    }

    /**
     * The GRASP mainframe. It consists of a loop, in which each iteration goes
     * through the constructive heuristic and local search. The best solution is
     * returned as result.
     *
     * @return The best feasible solution obtained throughout all iterations.
     */
    public Solution<E> solve() {
        bestSol = createEmptySol();
        for (int i = 0; i < iterations; i++) {
            constructiveHeuristic();
            localSearch();
            if (bestSol.cost > sol.cost) {
                bestSol = new Solution<E>(sol);
                if (verbose)
                    System.out.println("(Iter. " + i + ") BestSol = " + bestSol);
            }
        }

        return bestSol;
    }

    /**
     * A standard stopping criteria for the constructive heuristic is to repeat
     * until the current solution improves by inserting a new candidate
     * element.
     *
     * @return true if the criteria is met.
     */
    public Boolean constructiveStopCriteria() {
        return cost <= sol.cost;
    }
}
