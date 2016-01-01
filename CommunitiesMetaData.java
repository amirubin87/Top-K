import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

public class CommunitiesMetaData {
	
	UndirectedUnweightedGraph g;   
    public Map<Integer, Set<Integer>> com2nodes;
    public Map<Integer, Set<Integer>> node2coms;
    public Map<Integer, Integer> com2InnerNumOfEdges;
    public Map<Integer, Integer> com2BoundaryNumOfEdges;
    public Map<Integer, Map<Integer, Integer>> comPerNode2NumOfInnerEdges;
    
    public CommunitiesMetaData(){
    	com2nodes = new HashMap<Integer, Set<Integer>>();
    	node2coms = new HashMap<Integer, Set<Integer>>();
    	com2InnerNumOfEdges = new HashMap<Integer, Integer>();
    	com2BoundaryNumOfEdges = new HashMap<Integer, Integer>();
    	comPerNode2NumOfInnerEdges = new HashMap<Integer, Map<Integer, Integer>>();
    }
    
    public CommunitiesMetaData(UndirectedUnweightedGraph g, String pathToComms) throws IOException{
    	this();
    	this.g = g;
    	ListOfNodesToMappings(pathToComms);
    }
    
	public void ListOfNodesToMappings
	(String listsOfNodesPath) throws IOException{
		String[] listsOfNodes = (Files.readAllLines(Paths.get(listsOfNodesPath))).toArray(new String[0]);
        String comm = "";
        String[] nodes;
        SortedSet <Integer> nodesInt;
        Set<Integer> nodeComms;
        Set<Integer> neighbors;
        Map<Integer, Integer> node2numOfInnerEdges;
        int boundaryNumOfEdges=0;
        int innerNumOfEdges=0;
        int nodeInnerNumOfEdges=0;
        
        int countToDo = listsOfNodes.length;
		int tenPercent = countToDo/10+1;
		int commCounter = 0;
	    System.out.println();
		System.out.print("Preparing metadata. Progress: ");  	
			
        for(int commId = 0; commId<listsOfNodes.length ; commId++){
        	commCounter++;
			if ((commCounter%tenPercent) == 0){
        		System.out.print(commCounter/tenPercent*10 + "%  ");
        	}
        
            comm = listsOfNodes[commId];
            nodes = comm.replace("\t", " ").split(" ");
            if(nodes.length <3){
            	continue;
            }
            nodesInt = new TreeSet<Integer>();
            node2numOfInnerEdges = new HashMap<>();
            ConvertListOfStringToListOfInt(nodes, nodesInt);  
            // No duplicated comms
            if(com2nodes.values().contains(nodesInt))
            	continue;
            com2nodes.put(commId, nodesInt);            
            boundaryNumOfEdges= 0;
            innerNumOfEdges= 0;
            for (int node : nodesInt){
            	nodeInnerNumOfEdges=0;
            	nodeComms = node2coms.get(node);
            	if ( nodeComms == null){
            		nodeComms = new HashSet<Integer>();
            		node2coms.put(node, nodeComms);
            	}
            	nodeComms.add(commId);
            	//Calc num of edges and num of boundary edges
            	neighbors = g.neighbors(node);
            	for(int neighbor : neighbors){            		
            		if(nodesInt.contains(neighbor)){
            			nodeInnerNumOfEdges++;
            			if(neighbor < node){
            				innerNumOfEdges++;            				
            			}
            		}
        			else{
        				boundaryNumOfEdges++;
        			}
            	}
            	node2numOfInnerEdges.put(node, nodeInnerNumOfEdges);
            }
            com2BoundaryNumOfEdges.put(commId, boundaryNumOfEdges);
            com2InnerNumOfEdges.put(commId, innerNumOfEdges);    		
            comPerNode2NumOfInnerEdges.put(commId, node2numOfInnerEdges);
        }
        System.out.println();
	}
        
    private static void ConvertListOfStringToListOfInt(String[] nodes, SortedSet<Integer> nodesInt) {
		for (String node: nodes){
			nodesInt.add(Integer.parseInt(node));
		}
		
	}
    

}
