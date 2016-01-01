
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

public class CalcMetrics {
	
	public static SortedSet<Entry<Integer, Double>> Conductance(CommunitiesMetaData metaData){
		SortedSet<Entry<Integer, Double>> ans = new TreeSet<Entry<Integer, Double>>(new ValueComparator());
		int commId=0;
		int numOfInternalEdges=0;
		int numOfBoundaryEdges=0;
		double conductance=0;
		double dividor = 0;
		for(Entry<Integer, Integer> com2InnerNumOfEdges : metaData.com2InnerNumOfEdges.entrySet()){
			commId = com2InnerNumOfEdges.getKey();
			numOfInternalEdges = com2InnerNumOfEdges.getValue();
			numOfBoundaryEdges = metaData.com2BoundaryNumOfEdges.get(commId);
			dividor = 2*numOfInternalEdges+numOfBoundaryEdges;
			if(dividor ==0){
				conductance = 0;
			}
			else{
				conductance = 1-(double)numOfBoundaryEdges/(double)dividor;
			}
			// the higher the better!
			ans.add(new MyEntry(commId, conductance));
			
		}
		return ans;
	}

	public static SortedSet<Entry<Integer, Double>> FOMD(CommunitiesMetaData metaData){
		return FractionOverParam(metaData, true);		
	}
	
	public static SortedSet<Entry<Integer, Double>> FlakeODF(CommunitiesMetaData metaData){
		return FractionOverParam(metaData, false);
	}
	
	private static SortedSet<Entry<Integer, Double>> FractionOverParam(CommunitiesMetaData metaData, boolean useMedianDegree){		
		SortedSet<Entry<Integer, Double>> ans = new TreeSet(new ValueComparator());
		int commId=0;
		int nodeId=0;
		double compareTo=0;
		if (useMedianDegree){
			compareTo = metaData.g.MedianNodeDegree;
		}
		int countNodesWithLessThanCompareTo=0;
		double dividor = 0;
		// go over each comm
		for(Entry<Integer, Map<Integer, Integer>> comPerNodeNumOfInnerEdges : metaData.comPerNode2NumOfInnerEdges.entrySet()){
			countNodesWithLessThanCompareTo = 0;
			commId = comPerNodeNumOfInnerEdges.getKey();
			dividor = metaData.com2nodes.get(commId).size();			
			if(dividor==0){	
				ans.add(new MyEntry(commId, 0.0));
				continue;
			}
			// go over each node
			for(Entry<Integer, Integer> node2numOfInnerEdges : comPerNodeNumOfInnerEdges.getValue().entrySet()){
				
				nodeId = node2numOfInnerEdges.getKey();
				if (!useMedianDegree){
					compareTo = metaData.g.degree(nodeId)/2;				
					if(node2numOfInnerEdges.getValue() < compareTo){
						countNodesWithLessThanCompareTo++;
					}
				}
				else if(node2numOfInnerEdges.getValue() <= compareTo){
					countNodesWithLessThanCompareTo++;	
					
				}
			}
			ans.add(new MyEntry(commId, 1-(double)countNodesWithLessThanCompareTo/(double)dividor));
				
		}
		return ans;
	}

	public static SortedSet<Entry<Integer, Double>> TPM(CommunitiesMetaData metaData){
		SortedSet<Entry<Integer, Double>> ans = new TreeSet(new ValueComparator());
		int commId=0;
		Set<Integer> nodesInTriangle;
		double dividor = 0;
		Set<Integer> nodes;
		// go over each comm
		for(Entry<Integer, Set<Integer>> com2nodes : metaData.com2nodes.entrySet()){
			nodesInTriangle = new HashSet<>();
			commId = com2nodes.getKey();
			nodes = com2nodes.getValue();
			dividor = nodes.size();			
			if(dividor==0){	
				ans.add(new MyEntry(commId, 0.0));			
				continue;
			}
			// go over each node - notice that we count each node only once!
			for( int node : nodes){
				if(!nodesInTriangle.contains(node)){
				    Set<Integer> neighbours = metaData.g.neighbors(node);
				    Set<Integer> neighInComm = Utills.Intersection(nodes, neighbours);
				    for (int v : neighInComm){
				    	if (v != node){
					        for (int u : neighInComm){
					            if (u != v && u != node && metaData.g.get_edge_weight(u,v)>0){
					            	nodesInTriangle.add(node);
					            	nodesInTriangle.add(u);
					            	nodesInTriangle.add(v);				            	
					            }
					        }
				    	}
				    }
				}
			}
			ans.add(new MyEntry(commId, (double)nodesInTriangle.size()/(double)dividor));			
		}
		return ans;
	}

	public static SortedSet<Entry<Integer, Double>> CutRatio(CommunitiesMetaData metaData){
		SortedSet<Entry<Integer, Double>> ans = new TreeSet(new ValueComparator());
		int commId=0;
		double dividor = 0;
		int commSize = 0;
		int numOfBoundaryEdges=0;
		int n = metaData.g.number_of_nodes();
		// go over each comm
		for(Entry<Integer, Set<Integer>> com2nodes : metaData.com2nodes.entrySet()){
			commId = com2nodes.getKey();
			commSize = com2nodes.getValue().size();
			dividor = commSize*(n-commSize);		
			if(dividor==0){		
				ans.add(new MyEntry(commId, 0.0));				
				continue;
			}
			numOfBoundaryEdges = metaData.com2BoundaryNumOfEdges.get(commId);
			ans.add(new MyEntry(commId, 1- (double)numOfBoundaryEdges/dividor));	
		}
		return ans;
	}

	public static SortedSet<Entry<Integer, Double>> Modularity(CommunitiesMetaData metaData) throws Exception{
		SortedSet<Entry<Integer, Double>> ans = new TreeSet(new ValueComparator());
		int commId=0;
		int m = (int) metaData.g.number_of_edges();
		int numberOfInternalEdges=0;
		Set<Integer> nodes;
		double expectedNumberOfInternalEdges = 0;
		double newExpectedNumberOfInternalEdges = 0;
		// go over each comm
		for(Entry<Integer, Set<Integer>> com2nodes : metaData.com2nodes.entrySet()){
			nodes = com2nodes.getValue();
			 expectedNumberOfInternalEdges = 0;
			commId = com2nodes.getKey();
			numberOfInternalEdges = metaData.com2InnerNumOfEdges.get(commId);
			for(int v : nodes){
				double vDeg = metaData.g.degree(v);
				for(int u : nodes){
					//We go over each couple only once
					if(u>v){
						newExpectedNumberOfInternalEdges = expectedNumberOfInternalEdges + metaData.g.degree(u)*vDeg;
						if(expectedNumberOfInternalEdges > newExpectedNumberOfInternalEdges){
							throw new Exception("exceeded long value;");
						}
					}
				}
			}
			double modularity1 = (double)((double)numberOfInternalEdges - (double)expectedNumberOfInternalEdges/(double)(2*m))/(double)(2*m);
			
			double ei = (double)metaData.com2InnerNumOfEdges.get(commId)/(double)(2*m);
			double ai= (double)metaData.com2InnerNumOfEdges.get(commId)+(double)metaData.com2BoundaryNumOfEdges.get(commId)/(double)(2*m);
			double modularity2 = ei-Math.pow(ai, 2);
			if (modularity2- modularity1 >0.005){
				System.out.println("modularity2- modularity1 >0.005");
			}
			ans.add(new MyEntry(commId, modularity1));			
		}
		return ans;
	}
	
	public static SortedSet<Entry<Integer, Double>> Modularity2(CommunitiesMetaData metaData) throws Exception{
		SortedSet<Entry<Integer, Double>> ans = new TreeSet(new ValueComparator());
		int commId=0;
		int m = (int) metaData.g.number_of_edges();
		int numberOfInternalEdges=0;
		Set<Integer> nodes;
		double expectedNumberOfInternalEdges = 0;
		double newExpectedNumberOfInternalEdges = 0;
		// go over each comm
		for(Entry<Integer, Set<Integer>> com2nodes : metaData.com2nodes.entrySet()){
			commId = com2nodes.getKey();

			double ei = (double)metaData.com2InnerNumOfEdges.get(commId)/(double)(m);
			double ai= 2*(double)metaData.com2InnerNumOfEdges.get(commId)+(double)metaData.com2BoundaryNumOfEdges.get(commId)/(double)(2*m);
			double modularity2 = ei-Math.pow(ai, 2);

			ans.add(new MyEntry(commId, modularity2));			
		}
		return ans;
	}
}
