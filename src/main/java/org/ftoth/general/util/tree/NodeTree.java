package org.ftoth.general.util.tree;

/**
 * This is a node tree with a single root node.
 * 
 * @author ftoth
 */
public class NodeTree
{
	
	private static final int TREE_LEVEL_INDENT = 4;
	
	// ----------------- properties -----------------
	private Node root;
	
	public Node getRoot()
	{
		return root;
	}

	// ----------------- startup -----------------
	public NodeTree(Node root)
	{
		if (root == null) {
			throw new IllegalArgumentException("Cannot be created a NodeTree without root Node");
		}
		this.root = root;
	}
	
	// ----------------- functions -----------------
	/**
	 * It searches a need of this node tree.
	 * 
	 * @param node
	 * @return
	 */
	public boolean contains(Node node)
	{
		return containsThis(node, root);
	}
	
	/**
	 * To add a new node to the tree.
	 * 
	 * @param parent if null
	 * @param obj
	 * @return
	 * @throws IllegalArgumentException if you try to add more root nodes 
	 */
	public Node addObject(Node parent, String name, Object obj)
	{
		if (parent == null) {
			throw new IllegalArgumentException("You wanted to add [" + obj + "] to tree, but parent has not been specified");
		}
		if (!contains(parent)) {
			throw new IllegalArgumentException("You wanted to add [" + obj + "] to tree, but parent[" + parent + "] is not part of this tree"); 
		}
		Node node = new Node(name, obj); 
		parent.getChildren().add(node);
		return node;
	}
	
	// ----------------- override -----------------
	@Override
	public String toString()
	{
		StringBuffer b = new StringBuffer();
		getSubtreeAsString(b, root, 0);
		
		return b.toString();
	}
	
	// ----------------- helpers -----------------
	/**
	 * It searches a node in a node tree.
	 *  
	 * @param node
	 * @param rootOfSubtree
	 * @return
	 */
	private boolean containsThis(Node node, Node rootOfSubtree)
	{
		if (rootOfSubtree.equals(node)) {
			return true;
		}
		if (rootOfSubtree.getChildren() != null) {
			for (Node child : rootOfSubtree.getChildren()) {
				if (containsThis(node, child)) {
					return true;
				}
			}
		}
		return false;
	}
	
	void getSubtreeAsString(StringBuffer b, Node root, int level)
	{
		String offset = "";
		for (int n=0; n<level * TREE_LEVEL_INDENT; n++) {
			offset = offset + " ";
		}
		// first root of subtree
		if (level > 0) {
			b.append(offset + "|\n");
		}
		b.append(offset);
		if (root.getParentProperty() != null) {
			b.append(root.getParentProperty());
			if (root.getPropertyCollectionIndex() != null) {
				b.append("[" + root.getPropertyCollectionIndex() + "]");
			}
			b.append(" -> ");
		}
		b.append(root.getName());
		if (root.isLoopIndicator()) {
			b.append(" (LOOP)");
		}
		b.append("\n");
		
		// then children
		if (root.getChildren() != null) {
			for (Node child : root.getChildren()) {
				getSubtreeAsString(b, child, level + 1);
			}
		}
	}
}
