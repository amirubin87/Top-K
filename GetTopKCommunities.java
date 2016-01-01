
import java.io.FileNotFoundException;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

public class GetTopKCommunities {

	public static void main(String[] args) throws Exception {
		
		String pathToGraph = "C:/Users/t-amirub/Desktop/amazon/y/com-amazon.ungraph.txt";
		String pathToComms = "C:/Users/t-amirub/Desktop/amazon/y/com-amazon.all.cmty.txt";
		String outputPath = "C:/Users/t-amirub/Desktop/amazon/y/Top5000ByRank.txt";
		int k= 5000;
		
		if (args.length <4){
			System.out.println("Please give params: pathToGraph  pathToComms outputPath k");
			return;
		}
		else{					
				pathToGraph = args[0];		
				pathToComms = args[1];
				outputPath = args[2];	
				 k = Integer.parseInt(args[3]);		
		}
		
		System.out.println("pathToGraph: " + pathToGraph);
		System.out.println("pathToComms: " + pathToComms);		
		System.out.println("outputPath: " + outputPath);
		System.out.println("k: " + k);
		
		System.out.println("g");
		UndirectedUnweightedGraph g = new UndirectedUnweightedGraph(Paths.get(pathToGraph));		
		
		System.out.println("metaData");
		CommunitiesMetaData metaData = new CommunitiesMetaData(g,pathToComms);
		
		
		////////////////////////////
		// In all metrics- the first element must be the worst(lowest)!
		// Rank is: lower- better!
		///////////////////////
		
		System.out.println("Conductance");
		SortedSet<Entry<Integer, Double>> Conductance = CalcMetrics.Conductance(metaData);
		Map<Integer, Integer> ConductanceRank = sortedSetToMappingOfRanks(Conductance);
		System.out.println(ConductanceRank.size());
		
		System.out.println("FlakeODF");
		SortedSet<Entry<Integer, Double>> FlakeODF = CalcMetrics.FlakeODF(metaData);
		Map<Integer, Integer> FlakeODFRank = sortedSetToMappingOfRanks(FlakeODF);
		System.out.println(FlakeODFRank.size());
		
		System.out.println("FOMD");
		SortedSet<Entry<Integer, Double>> FOMD = CalcMetrics.FOMD(metaData);
		Map<Integer, Integer> FOMDRank = sortedSetToMappingOfRanks(FOMD);
		System.out.println(FOMDRank.size());
		
		System.out.println("TPM");
		SortedSet<Entry<Integer, Double>> TPM = CalcMetrics.TPM(metaData);
		Map<Integer, Integer> TPMRank = sortedSetToMappingOfRanks(TPM);
		System.out.println(TPMRank.size());
		
		System.out.println("CutRatio");
		SortedSet<Entry<Integer, Double>> CutRatio = CalcMetrics.CutRatio(metaData);
		Map<Integer, Integer> CutRatioRank = sortedSetToMappingOfRanks(CutRatio);
		System.out.println(CutRatioRank.size());
		
		System.out.println("Modularity2");
		SortedSet<Entry<Integer, Double>> Modularity2 = CalcMetrics.Modularity2(metaData);
		Map<Integer, Integer> Modularity2Rank = sortedSetToMappingOfRanks(Modularity2);
		System.out.println(Modularity2Rank.size());
		
		System.out.println("MedianRank");
		Map<Integer, Integer> MedianRank = MedianRank(ConductanceRank, FlakeODFRank, FOMDRank, TPMRank, CutRatioRank, Modularity2Rank);
		
		System.out.println("top " + k);		
		Map<Integer, Set<Integer>> topK = GetTopK(MedianRank, k, metaData.com2nodes);

		System.out.println("WriteToFile");
		WriteToFile(topK, outputPath);
	}

	private static int CompreRanks(Map<Integer, Integer> modularityRank,
			Map<Integer, Integer> modularity2Rank) {
		int ans = 0;
		for ( Integer id : modularityRank.keySet()){
			if(modularityRank.get(id) != modularity2Rank.get(id)){
				ans++;
			}
		}
		return ans;
	}

	// the first element in the set gets the highest rank!
	// So the first must be the worst.
	public static Map<Integer, Integer> sortedSetToMappingOfRanks (SortedSet<Entry<Integer, Double>> sorted){
		Map<Integer, Integer> ans = new HashMap<>();
		Iterator<Entry<Integer, Double>> iter = sorted.iterator();
		int size = sorted.size();
		int commId;
		while (iter.hasNext()){
			commId = iter.next().getKey();
			ans.put(commId, size);
			size--;
		}
		return ans;
	}
	
	private static Map<Integer, Set<Integer>> GetTopK(Map<Integer, Integer> SumRank, int k, Map<Integer, Set<Integer>> com2nodes) {
		if(com2nodes.size()<k){
			return com2nodes;
		}
		
		Map<Integer, Set<Integer>> ans = new HashMap<>();
		ArrayList<Integer> list = new ArrayList<Integer>(SumRank.values());
		// smallest first!
		Collections.sort(list);
		
		double kthValue = list.get(k);
		for(Entry<Integer, Integer> commIdAndSum : SumRank.entrySet()){			
			int commId= commIdAndSum.getKey();
			if(commIdAndSum.getValue()<=kthValue){
				ans.put(commId, com2nodes.get(commId));
			}
		}		
		return ans;
	}
	
	

	private static Map<Integer, Integer> SumRank(Map<Integer, Integer> conductance, Map<Integer, Integer> flakeODF,
			Map<Integer, Integer> FOMD, Map<Integer, Integer> TPM, Map<Integer, Integer> cutRatio,
			Map<Integer, Integer> modularity) {
		Map<Integer, Integer> ans = new HashMap<>();
		for(Integer commId : conductance.keySet()){
			ans.put(commId, 
					conductance.get(commId).intValue() + flakeODF.get(commId).intValue() + FOMD.get(commId).intValue() + TPM.get(commId).intValue() + cutRatio.get(commId).intValue() + modularity.get(commId).intValue());
		}
		return ans;
	}
	
	private static Map<Integer, Integer> MedianRank(Map<Integer, Integer> conductance, Map<Integer, Integer> flakeODF,
			Map<Integer, Integer> FOMD, Map<Integer, Integer> TPM, Map<Integer, Integer> cutRatio,
			Map<Integer, Integer> modularity) {
		Map<Integer, Integer> ans = new HashMap<>();
		for(Integer commId : conductance.keySet()){
			ans.put(commId, 
					CalcMedian(conductance.get(commId).intValue(),flakeODF.get(commId).intValue(),FOMD.get(commId).intValue(),TPM.get(commId).intValue(),cutRatio.get(commId).intValue(),modularity.get(commId).intValue()));
		}
		return ans;
	}
	
	

	private static Integer CalcMedian(int intValue, int intValue2, int intValue3, int intValue4, int intValue5,
			int intValue6) {
		int[] vals= new int[6];
		vals[0] = intValue;
		vals[1] = intValue2;
		vals[2] = intValue3;
		vals[3] = intValue4;
		vals[4] = intValue5;
		vals[5] = intValue6;
		Arrays.sort(vals);
		return vals[vals.length/2];
	}

	private static void WriteToFile(Map<Integer, Set<Integer>> comms, String outputPath) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(outputPath , "UTF-8");
		for ( Set<Integer> listOfNodes : comms.values()){
			if(listOfNodes.size()>0){
				for(int node : listOfNodes){
					writer.print(node + " ");
				}
				writer.println("");
			}
		}		
		writer.close();	
	}
}

