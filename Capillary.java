// This tree uses a bitwise traversal of the branches using
// keys to access the associated values. It stores common
// common prefixes (rapids) in each nodes to reduce the size 
// and costs to function in a similar manner to radix trees.

import java.util.*;

public class Capillary<V>{
	// Basic Node. Only Root should be a node
	private class Node{
		Node child = null;
		BitSet rapids = null;
		int rLength = 0;
	}
	// Branches are like Nodes but have 
	// an additional pointer to go "left"
	private class Branch extends Node{
		Node zero = null;
	}

	// Leaves are like node but hold a
	// pointer to the "value" object
	private class Leaf extends Node{
		V value;

		Leaf(V newValue){
			value = newValue;
			child = null;
		}
	}

	Node root;
	int height; // This is the length of the key in bits
	int size = 0;

	// Tree MUST be initialized with the
	// length of the hash code used for keying
	public Capillary(int h){
		root = new Node();
		height = h;
	}

	// If the tree is empty, we simply create a 
	// new leaf at the root. Otherwise we irrigate
	public void put(V value, BitSet hash){
		if (isEmpty()){
			root.child = new Leaf(value);
			root.child.rapids = (BitSet) hash.clone();
			root.child.rLength = height;
		} else
			irrigate(hash, value);
		size++;
	}

	public int getSize(){
		return size;
	}

	// Navigates the whole tree wtht a tolerance of 100%
	// This is a comprehensive traversal, DFS wise.
	public List<V> values(){
		List<V> matches = new ArrayList<V>();
		BitSet hash = new BitSet();
		if (!isEmpty())
			navigate(hash, root.child, height, height, matches);
		return matches;
	}

	// Creates a list of values with keys within
	// bitwise tolerance of received hash
	public List<V> search(BitSet hash, int tolerance){
		List<V> matches = new ArrayList<V>();
		if (!isEmpty())
			navigate(hash, root.child, height, tolerance, matches);
		return matches;
	}

	public boolean isEmpty(){
		return (root.child == null);
	}

	private void irrigate(BitSet map, V value){
		// We start with the root as the parent node
		int cHeight = height;
		Node cNode = root;
		boolean lBranch = false;

		// If we are not at leaf level
		while(cHeight > 0){
			// We check if the child is at a left branch
			Node cChild = null;
			if(!lBranch)
				cChild = cNode.child;
			else
				cChild = ((Branch) cNode).zero;

			BitSet cCompare = null;
			// If that child has a rapid
			if (cChild.rLength > 0){
				// We compare the rapids
				cCompare = map.get(cHeight-cChild.rLength,cHeight);
				cCompare.xor(cChild.rapids);
			}
			// If there were no rapids or no difference
			if (cCompare == null || cCompare.isEmpty()){
				// We navigated the length of the rapids
				cHeight -= cChild.rLength;
				// If we reached leaf level, loop is over
				if (cHeight == 0 ) break;
				// Otherwise, we evaluate the branch
				lBranch = !map.get(--cHeight);
				// And our current child becomes the parent
				cNode = cChild;
			} else{ // If there were a difference in rapids, we split
				// Split occurs at the highest bit of difference
				int brLoc = cCompare.length();
				// We create a new leaf and a new branch
				Branch nBranch = new Branch();
				Leaf nLeaf = new Leaf(value);

				// If the split is not at the very beginning
				// The new branch will have upper part of the rapids
				if (brLoc < cChild.rLength){
					nBranch.rapids = cChild.rapids.get(brLoc,cChild.rLength);
					nBranch.rLength = cChild.rLength - brLoc;
				}
				// If the split is not at leaf level
				if ((cHeight+brLoc-cChild.rLength) > 1){
					// The new leaf will have rapids equal to 
					// its map up to height of the split
					nLeaf.rapids = map.get(0,cHeight-cChild.rLength+brLoc-1);
					nLeaf.rLength = cHeight-cChild.rLength+brLoc-1;
				}
				// Use map to see if the old path should go right
				// at the split or vice versa
				if (map.get(cHeight-cChild.rLength+brLoc-1)){
					nBranch.child = nLeaf;
					nBranch.zero = cChild;
				} else{
					nBranch.child = cChild;
					nBranch.zero = nLeaf;
				}
				// If the split is not at the very end
				if (brLoc > 1){
					// Current child will keep only lower part of rapids
					cChild.rapids = cChild.rapids.get(0,brLoc-1);
					cChild.rLength = brLoc-1;
				} else{
					// Otherwise, it doesn't have any rapids
					cChild.rapids = null;
					cChild.rLength = 0;
				}
				// Then we check if the current child is a left or 
				// right child and place the new branch accordingly.
				if (cNode.child == cChild){
					cNode.child = nBranch;
				}
				else{
					((Branch) cNode).zero = nBranch;
				}
				// After splitting the rapids, we're done
				return;
			}
		}
		// If we get here, we found an exact match in the map
		// We chose the child according to previous branch
		Leaf cLeaf = null;
		if(!lBranch)
				cLeaf = (Leaf) cNode.child;
		else
			cLeaf = (Leaf) ((Branch) cNode).zero;
		// Then we look for next available spot to create a new leaf
		while (cLeaf.child != null)
			cLeaf = (Leaf) cLeaf.child;
		cLeaf.child = new Leaf(value);
	}

	private void navigate(BitSet map, Node start, int drop, int tolkens, List<V> matches){
		// Set current navigation data from received specifications
		Node cNode = start;
		int cHeight = drop;
		int cTolkens = tolkens;

		// While we're not at leaf level
		while (cHeight > 0){
			// If we hit a rapid
			if (cNode.rLength > 0){
				// We compare that portion of the map with the rapids
				BitSet cCompare = map.get(cHeight-cNode.rLength,cHeight);
				cCompare.xor(cNode.rapids);

				// Every difference reduces our tolkens
				cTolkens -= cCompare.cardinality();

				// If tokens are negative, this boat is broken
				if (cTolkens < 0) return;

				// Otherwise we dropped by length of rapids
				cHeight -= cNode.rLength;
			}
			// If we're still not a leaf level, we have a branch
			if (cHeight > 0){
				// We look at the map
				if (map.get(--cHeight)){
					// If we map says to go right but we have tolkens left,
					if (cTolkens > 0)
						navigate(map,((Branch) cNode).zero,cHeight,cTolkens-1,matches);
					// We navigate left first then we go right
					cNode = cNode.child;
				} else{
					// If we map says to go left but we have tolkens left,
					if (cTolkens > 0)
						navigate(map,cNode.child,cHeight,cTolkens-1,matches);
					// We navigate right first then we go right
					cNode = ((Branch) cNode).zero;
				}
			}
		}

		// If we reach leaf level, we store all the values in the list
		Leaf cLeaf = (Leaf) cNode;
		while (cLeaf != null){
			matches.add(cLeaf.value);
			cLeaf = (Leaf) cLeaf.child;
		}
	}
}
