package org.ftoth.general.util.tree;

import java.util.List;

/**
 * @author ftoth
 *
 */
public class Node implements Comparable<Node>
{
	// ----------------- properties -----------------
	private String name;
	private String parentProperty;
	private Integer propertyCollectionIndex;
	private Object data;
	private List<Node> children;
	private boolean loopIndicator = false;
	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Object getData()
	{
		return data;
	}
	
	public void setData(Object data)
	{
		this.data = data;
	}
	
	public List<Node> getChildren()
	{
		return children;
	}
	
	public void setChildren(List<Node> children)
	{
		this.children = children;
	}

	public boolean isLoopIndicator()
	{
		return loopIndicator;
	}

	public void setLoopIndicator(boolean loopIndicator)
	{
		this.loopIndicator = loopIndicator;
	}
	
	public String getParentProperty()
	{
		return parentProperty;
	}

	public void setParentProperty(String parentProperty)
	{
		this.parentProperty = parentProperty;
	}

	public Integer getPropertyCollectionIndex()
	{
		return propertyCollectionIndex;
	}

	public void setPropertyCollectionIndex(Integer propertyCollectionIndex)
	{
		this.propertyCollectionIndex = propertyCollectionIndex;
	}

	// ----------------- startup -----------------
	public Node(String name, Object data)
	{
		if (name == null) {
			throw new IllegalArgumentException("You cannot create a node with empty name");
		}
		if (data == null) {
			throw new IllegalArgumentException("You cannot create a node with empty data");
		}
		this.name = name;
		this.data = data;
	}
	
	public Node(Node cloned)
	{
		this.name = cloned.getName();
		this.data = cloned.getData();
		this.parentProperty = cloned.getParentProperty();
	}
	
	// ----------------- override -----------------
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 * 
	 * It compares name and data of nodes.
	 */
	@Override
	public boolean equals(Object obj)
	{
		Node otherNode = (Node)obj;
		return otherNode.getName().equals(name) && otherNode.getData().equals(data);
	}

	@Override
	public int compareTo(Node other)
	{
		return name.compareTo(other.getName());
	}

	@Override
	public String toString()
	{
		return "[" + name + "] - (" + data.getClass().getName() + ")";		
	}
	
	
}
