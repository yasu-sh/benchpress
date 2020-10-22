
import jpsgcs.alun.jtree.DiscreteDataMatrix;
import jpsgcs.alun.jtree.WSMGraphLaw;
import jpsgcs.alun.jtree.SMGraphLaw;
import jpsgcs.alun.jtree.MultinomialMarginalLikelihood;
import jpsgcs.alun.jtree.EdgePenaltyPrior;
import jpsgcs.alun.jtree.ProductGraphLaw;
import jpsgcs.alun.jtree.JTree;
import jpsgcs.alun.jtree.JTreeSampler;
import jpsgcs.alun.jtree.JTreeExpander;
import jpsgcs.alun.jtree.GiudiciGreen;
import jpsgcs.alun.jtree.UniformDecomposable;
import jpsgcs.alun.jtree.MultiPairJTree2;
import jpsgcs.alun.graph.Network;
import jpsgcs.alun.viewgraph.GraphFrame;
import jpsgcs.alun.markov.Parameter;
import jpsgcs.alun.markov.Variable;
import jpsgcs.alun.util.Monitor;
import jpsgcs.alun.util.ArgParser;

import java.util.ConcurrentModificationException;
import java.util.Random;

public class EstimateMultinomialGM
{
	public static void main(String[] args)
	{
		try
		{
		// Create random number generator.

			Random rand = new Random();

		// Set the parameters.

			ArgParser ap = new ArgParser(args);

			boolean showgraph = !ap.gotOpt("-v");
			int n_samples = ap.intAfter("-n",10000000);
			int sampler = ap.intAfter("-s",0); 
			int randomits = ap.intAfter("-r",1000);
			double penalty = ap.doubleAfter("-p",0);

		// Read the command line arguments.

			DiscreteDataMatrix data = new DiscreteDataMatrix();

		// Create a graph of Variables for the conditional independence graph
		// and find a junction tree of the graph.

			Network<Variable,Object> g = new Network<Variable,Object>();
			Variable[] vars = new Variable[data.nColumns()];
			for (int i=0; i<data.nColumns(); i++)
			{
				vars[i] = new Variable(i,data.statesOfColumn(i));
				g.add(vars[i]);
			}

			JTree<Variable> jt = new JTree<Variable>(g,rand);

		// Make a prior that penalizes the number of edges in the graph.
		// Default is to have no penalty.

			WSMGraphLaw<Variable> prior = new EdgePenaltyPrior<Variable>(penalty);

		// Make a Multinoimal marginal likelihood from the data.

			SMGraphLaw<Variable> likelihood = new MultinomialMarginalLikelihood(vars,data,1);

		// Make a posterior by combining the prior and likelihood.

			WSMGraphLaw<Variable> posterior = new ProductGraphLaw<Variable>(prior,likelihood);
			
		// Create a junction tree sampler to sample from the poserior.

		 	JTreeSampler<Variable> jts = null;

			switch(sampler)
			{
			// Samples uniformly on decomposable graphs.
			case 2: jts = new GiudiciGreen<Variable>(jt,posterior);
				break;

			// Samples uniformly on decomposable graphs.
			case 1: jts = new UniformDecomposable<Variable>(jt,posterior);
				break;
			
			// Samples uniformly on decomposable graphs.
			case 0:
			default:
				jts = new MultiPairJTree2<Variable>(jt,posterior,true);
			}

		// Randomize the initial junction tree.

			jts.randomize();

		// Create a slider bar to control annealing temperature. Scale is %.
		// That is parameter value of 100 will set temperature to 1.

			Parameter temp = new Parameter("Temperature",0,1000,100);

		// Create the graphial interface elements.

			GraphFrame frame = null;
			if (showgraph)
				frame = new GraphFrame<Variable,Object>(g,temp);

		// Run the sampler.

			Monitor.restart();
            //Monitor.show("Start");
            String max_neighlist = g.toString();
            Double max_prob = posterior.logProbability(jt);
		
			for (int i=1; i<=n_samples; i++)
			{
				if (i % 10000 == 0)
					System.err.print(".");
				if (i % 1000000 == 0)
					System.err.println();

				jts.setTemperature(temp.getValue()/100.0);

				if (i % randomits == 0)
				{
					jts.randomize();
					//System.out.println(posterior.logProbability(jt));
				}

                jts.randomUpdate();
                if (posterior.logProbability(jt) > max_prob) {
                    max_prob = posterior.logProbability(jt);
                    max_neighlist = g.toString();
                }
			}
			System.out.println(max_neighlist);
			//System.out.println(g);

			//Monitor.show("Done");
		}
		catch (ConcurrentModificationException cme)
		{
		}
		catch (Exception e)
		{
			System.err.println("Caught in EstimateMultinomialGM.main()");
			e.printStackTrace();
		}
	}
}
