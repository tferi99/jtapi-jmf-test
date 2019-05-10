package org.ftoth.general.util.spring;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ftoth.general.util.tree.Node;
import org.ftoth.general.util.tree.NodeTree;
import org.springframework.context.ApplicationContext;

public class SpringContextDump
{
	private final static Log log = LogFactory.getLog(SpringContextDump.class);
	
	public static String dumpSpringContext(ApplicationContext springAppContext)
	{
		StringBuilder buf = new StringBuilder();
		
		// getting beans from context and sorting into alphabet
		String[] nameArr = springAppContext.getBeanDefinitionNames();
		List<Node> beanNodes = new ArrayList<Node>();
		for (String name : nameArr) {
			Object bean = springAppContext.getBean(name);
			if (bean == null) {
				throw new RuntimeException("Error during getting bean[" + name + "]");
			}
			beanNodes.add(new Node(name, bean));
		}
		Collections.sort(beanNodes);
		if (log.isDebugEnabled()) {
			log.debug("Found " + beanNodes.size() + " bean(s).");
		}
		
		// hash map to get node by object efficiently 
		Map<Object, Node> beanNodeMap = new Hashtable<Object, Node>();
		for (Node beanNode : beanNodes) {
			beanNodeMap.put(beanNode.getData(), beanNode);
		}
		// hash set to check object type existence efficiently
		Set<Class<?>>beanTypes = new HashSet<Class<?>>();
		for (Node beanNode : beanNodes) {
			beanTypes.add(beanNode.getData().getClass());
		}
		// iterate on beans and build bean subtrees
		List<NodeTree>trees = new ArrayList<NodeTree>(); 
		for (Node beanNode : beanNodes) {
			NodeTree subTree = createSubTreeForBean(beanNode, beanNodeMap, beanTypes);
			if (subTree != null) {
				trees.add(subTree);
			}
		}
		
		// remove subtree duplications, only the biggest independent trees
		// will be stay here
		for (Iterator<NodeTree> i = trees.iterator(); i.hasNext();) {
			NodeTree target = i.next();
			for (NodeTree t : trees) {
				if (target.equals(t)) {
					continue;		// don't compare to itself
				}
				// if another tree contains a root of this subtree
				// it has to be removed
				if (t.contains(target.getRoot())) {
					i.remove();
					break;		// get next tree to examine
				}
			}
		}
		
		
		// output
		buf.append("======================== All Spring Beans =======================\n");
		buf.append("Number of beans: " + beanNodes.size() + "\n");
		for(Node bean : beanNodes) {
			buf.append(" - " + bean + "\n");
		}
		buf.append("================== Spring Bean Dependencies  =====================\n");
		for (int n=0; n<trees.size(); n++) {
			NodeTree tree = trees.get(n);
			buf.append("TREE[" + n + "]\n");
			buf.append(tree);
			
		}
		buf.append("==================================================================\n");

		return buf.toString();
		
	}
	
	// ----------------- helpers -----------------
	/**
	 * It returns bean subtree from context from specified
	 * node. If subtree contains only specified bean as root
	 * node, null will be returned (since it's not a real tree).
	 *   
	 * @param bean
	 * @param beanNodeMap to decide if an object is a Spring bean
	 * @param beanTypes 
	 * @return
	 */
	private static NodeTree createSubTreeForBean(Node bean, Map<Object, Node> beanNodeMap, Set<Class<?>> beanTypes)
	{
		if (log.isDebugEnabled()) {
			log.debug("Creating subtree for " + bean);
		}
		NodeTree subTree = new NodeTree(bean);
		
		addChildrenOfNode(subTree, bean, beanNodeMap, beanTypes);
		if (bean.getChildren() == null || bean.getChildren().size() == 0) {
			return null;
		}
		return subTree; 
	}

	/**
	 * To add children of a node to a tree.
	 * We pass tree itself as argument to check of loop references.
	 * 
	 * @param subTree
	 * @param beanNode
	 * @param beanNodeMap
	 * @param beanTypes 
	 */
	private static void addChildrenOfNode(NodeTree subTree, Node beanNode, Map<Object, Node> beanNodeMap, Set<Class<?>> beanTypes)
	{
		Object bean = beanNode.getData();
		PropertyDescriptor[] props = PropertyUtils.getPropertyDescriptors(bean);
		// loop on properties of current bean
		for (PropertyDescriptor prop : props) {
			try {
				Class<?> propType = prop.getPropertyType();				
				// if property is a collection
				if (Arrays.asList(propType.getInterfaces()).contains(Collection.class)) {
					if (log.isTraceEnabled()) {
						log.trace("Property [" + prop.getName() + "] is a Collection");
					}
					Object object = PropertyUtils.getSimpleProperty(bean, prop.getName());
					if (object != null) {
					//if (object instanceof Collection<?>) {
						Collection<?> childObjects = (Collection<?>)object;
						int idx = 0;
						for (Object childObject : childObjects) {
							Node child = beanNodeMap.get(childObject);
							// if it's a Spring bean that found in a collection
							if (child != null) {
								addChild(subTree, beanNode, child, prop, beanNodeMap, beanTypes, idx);
							}
							idx++;
						}
					}
				}
				else {		// not a collection
					// checking type of getter before calling it, to eliminate
					// unnecessary calls
					if (beanTypes.contains(propType)) {				
						if (log.isTraceEnabled()) {
							log.trace("Property [" + prop.getName() + "] contains a Spring bean"); 
						}
						// run getter property only if type exist is type set 
						Object object = PropertyUtils.getSimpleProperty(bean, prop.getName());
						if (object != null) {
							// getting child Spring beans
							// checking if this object is member of bean map
							Node child = beanNodeMap.get(object);
							// if it's a Spring bean
							if (child != null) {
								addChild(subTree, beanNode, child, prop, beanNodeMap, beanTypes, null);
							}
						}
					}
					else {
						if (log.isTraceEnabled()) {
							log.trace("Property [" + prop.getName() + "] of " + beanNode + " ignored.");
						}
					}
				}
			}
			catch (IllegalAccessException e) {
				continue;
			}
			catch (InvocationTargetException e) {
				continue;			
			}
			catch (NoSuchMethodException e) {
				continue;			
			}
		}
	}

	private static void addChild(NodeTree subTree, Node beanNode, Node child, PropertyDescriptor prop, Map<Object, Node> beanNodeMap, Set<Class<?>> beanTypes, Integer propertyCollectionIndex)
	{
		if (beanNode.getChildren() == null) {
			beanNode.setChildren(new ArrayList<Node>());
		}
		Node nodeWithProp = new Node(child);
		nodeWithProp.setParentProperty(prop.getName());
		if (propertyCollectionIndex != null) {
			nodeWithProp.setPropertyCollectionIndex(propertyCollectionIndex);
		}
		beanNode.getChildren().add(nodeWithProp);
		
		// checking looping references
/*	LOOPING STILL NOT SUPPORTED					if (subTree.contains(nodeWithProp)) {
			nodeWithProp.setLoopIndicator(true);
		}
		else {
			// add children of children, too
			addChildrenOfNode(subTree, nodeWithProp, beanNodeMap);
		}*/
		addChildrenOfNode(subTree, nodeWithProp, beanNodeMap, beanTypes);
	}
	
}
