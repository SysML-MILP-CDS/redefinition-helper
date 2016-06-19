/**
 * Copyright (c) 2015, Model-Based Systems Engineering Center, Georgia Institute of Technology.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 * 
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *   
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.gatech.mbse.plugins.papyrus.redefinition.popup.handlers;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;

/**
 * Helper functions for transformation: these are sterotype specific.
 * 
 * @author Sebastian
 * @version 0.0.1
 */
public class TransformationHelper {

	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(TransformationHelper.class.getName());
	
	/**
	 * Checks whether a particular resource is relevant within the context of a specific
	 * process.
	 * 
	 * @param resource The resource to query for
	 * @param topLevelActivity The top level activity
	 * @param rootElement The root element of the model tree
	 * @return <b>true</b> if the resource is relevant, <b>false</b> otherwise
	 */
	public static boolean isResourceRelevant(Element resource, Activity topLevelActivity,
			Element rootElement) {
		boolean found = false;
		
		ArrayList<Activity> allWPs = TransformationHelper.getWorkingPrinciples(rootElement);
		
		// TODO We can optimize this by caching the relevant resources and then checking whether the
		//      resource queried for is a part of that set.
		// TODO A little slow currently... goes through activities twice or more potentially
		
		// Collect activities - order is not important
		ArrayList<Activity> activities = collectSubActivities(topLevelActivity);
		
		for (Activity act : activities) {
		//for (Element cba : topLevelActivity.getOwnedElement()) {
			//if (cba instanceof CallBehaviorAction) {
				// TODO Nested activities
				//Activity act = (Activity) ((CallBehaviorAction) cba).getBehavior();
				
			// Find working principles associated with it
			ArrayList<Activity> workingPrinciples = 
					TransformationHelper.getWorkingPrinciples(act, allWPs);
			
			// Find resources associated with it
			HashSet<Element> resources =
					TransformationHelper.getResourceTypes(workingPrinciples);
			
			// Check whether it directly contains the resource
			if (resources.contains(resource))
				return true;
			
			// Otherwise do a check with the oneBaseClassifier function
			for (Element r : resources) {
				if (TransformationHelper.oneBaseClassifierIsSpecificResource(resource, (Classifier) r)
						|| TransformationHelper.oneBaseClassifierIsSpecificResource(r, (Classifier) resource))
					return true;
			}
			//}
		//}
		}
		
		return found;
	}
	
	/**
	 * Returns the constraints associated with a particular working principle.
	 * <P>
	 * This will return all constraints associated with a working principle at any level of
	 * inheritance.
	 * 
	 * @param workingPrinciple
	 * @return
	 */
	public static ArrayList<Constraint> collectConstraints(Activity workingPrinciple) {
		ArrayList<Constraint> constraints = new ArrayList<Constraint>();
		
		// Owned constraints
		for (NamedElement n : workingPrinciple.getOwnedMembers()) {
			if (n instanceof Constraint) {
				constraints.add((Constraint) n);
			}
		}
		
		// Inherited constraints
		for (NamedElement n : workingPrinciple.getInheritedMembers()) {
			if (n instanceof Constraint) {
				constraints.add((Constraint) n);
			}
		}
		
		return constraints;
	}
	
	/**
	 * Parses an activity by collecting all of the subactivities from each.
	 * 
	 * @param activity
	 * @return
	 */
	public static ArrayList<Activity> collectSubActivities(Activity activity) {
		ArrayList<Activity> activities = new ArrayList<Activity>();
		
		if (activity.getOwnedElements() != null) {
			for (Element cba : activity.getOwnedElements()) {
				if (cba instanceof CallBehaviorAction) {
					// FIXME The behavior could also be an opaque behavior
					if (((CallBehaviorAction) cba).getBehavior() instanceof Activity) {
						Activity subActivity = (Activity) ((CallBehaviorAction) cba).getBehavior();
						activities.add(subActivity);
						activities.addAll(collectSubActivities(subActivity));
					}
				}
			}
		}
		
		return activities;
	}
	
	/**
	 * Returns the non-abstract machining resources.
	 * 
	 * @return
	 */
	public static ArrayList<Element> getConcreteResources(Element startingElement, 
			Activity topLevelActivity, Element rootElement) {
		ArrayList<Element> resources = new ArrayList<Element>();
		
		// Iterate through owned elements to find resources
		for(Element e : startingElement.getOwnedElements()) {
			// Depth first search
			if(e.getOwnedElements() != null)
				resources.addAll(getConcreteResources(e, topLevelActivity, rootElement));
			
			if(e instanceof Classifier
					//&& oneBaseClassifierIsResource(e)
					&& isResource(e)
					&& !isAbstract((Classifier) e)
					&& isResourceRelevant(e, topLevelActivity, rootElement)) {
				logger.trace("Looks like a concrete resource: " + ((Classifier) e).getName());
				
				resources.add(e);
			}
		}
		
		return resources;
	}
	
	/** @see TransformationHelper#getConcreteResources(Element, Activity, Element) */
	public static ArrayList<Element> getConcreteResources(Element rootElement, Activity topLevelActivity) {
		return getConcreteResources(rootElement, topLevelActivity, rootElement);
	}
	
	/**
	 * Returns a list of all types of resources (abstract and non-abstract)
	 * 
	 * @return
	 */
	public static ArrayList<Element> getAllResources(Element startingElement, 
			Activity topLevelActivity, Element rootElement) {
		ArrayList<Element> resources = new ArrayList<Element>();
		
		// Iterate through owned elements to find resources
		for (Element e : startingElement.getOwnedElements()) {
			// Depth first search
			if (e.getOwnedElements() != null)
				resources.addAll(getAllResources(e, topLevelActivity, rootElement));
			
			// FIXME Fairly slow right now due to a repeated search through the model - pre-compute the relevant resources
			if (e instanceof Classifier
					//&& oneBaseClassifierIsResource(e)
					&& isResource(e)
					&& isResourceRelevant(e, topLevelActivity, rootElement)) {
				logger.trace("Looks like a resource: " + ((Classifier) e).getName());
				
				resources.add(e);
			}
		}
		
		return resources;
	}
	
	/** @see TransformationHelper#getAllResources(Element, Activity, Element) */
	public static ArrayList<Element> getAllResources(Element rootElement, Activity topLevelActivity) {
		return getAllResources(rootElement, topLevelActivity, rootElement);
	}
	
	/**
	 * Returns a list of composite resources in the provided list of resources.
	 * 
	 * @param resources
	 * @return
	 */
	public static ArrayList<Element> getCompositeResources(ArrayList<Element> resources) {
		ArrayList<Element> composites = new ArrayList<Element>();
		
		// Go through list of resources (concrete) and add if composite
		for (Element e : resources) {
			if (TransformationHelper.isCompositeResource(e))
				composites.add(e);
		}
		
		return composites;
	}
	
	/**
	 * Returns the non-abstract machining resources.
	 * 
	 * @return
	 */
	public static ArrayList<Element> getConcreteResourcesForAbstractResourceType(ArrayList<Element> concreteResources, Element abstractResource) {
		ArrayList<Element> resources = new ArrayList<Element>();
		
		if (abstractResource instanceof Classifier
				&& !TransformationHelper.isAbstract((Classifier) abstractResource))
			resources.add(abstractResource);
		
		// Iterate through owned elements to find resources
		for (Element e : concreteResources) {
			// Depth first search
			if (!e.equals(abstractResource)
					&& oneBaseClassifierIsSpecificResource(e, (Classifier) abstractResource)) {
				resources.add(e);
			}
		}
		
		return resources;
	}
	
	/**
	 * Returns an in-order version of the activity.
	 * <p>
	 * This function will parse the given activity and follow a workpiece.
	 * 
	 * @param start
	 * @return
	 */
	public static ArrayList<CallBehaviorAction> getInOrderProcess(Activity start) {
		//TODO For parallel branches, we could simply add an ArrayList<CallBehaviorAction> as an element?
		ArrayList<CallBehaviorAction> process = new ArrayList<CallBehaviorAction>();
		
		// Find starting point - this can be one of two things: an IN parameter, or a
		// start node
		for (Parameter p : start.getOwnedParameters()) {
			if (p.getDirection() == ParameterDirectionKind.IN_LITERAL
					&& isWorkpiece(p.getType())) {
				
			}
		}
		
		// Now follow the object flow to the next callbehavioraction (or other element)
		// 	For merges: take outflow
		//	For decision points: simulate process to reach condition (note: simple conditions supported only)
		//		--> use properties of activities
		
		return process;
	}
	
	/**
	 * Collect all working principles in the project.
	 * 
	 * @param rootElement
	 * @return
	 */
	public static ArrayList<Activity> getWorkingPrinciples(Element rootElement) {
		ArrayList<Activity> wps = new ArrayList<Activity>();
		
		if(rootElement.getOwnedElements() != null) {
			for(Element e : rootElement.getOwnedElements()) {
				if(TransformationHelper.isWorkingPrinciple(e))
					wps.add((Activity) e);
				
				wps.addAll(getWorkingPrinciples(e));
			}
		}
		
		return wps;
	}
	
	/**
	 * Returns a list of all working principles associated with a particular
	 * activity.
	 * <P>
	 * The function returns a set with the activity if the specified activity is a working
	 * principle itself. An empty set is returned if no working principles can be
	 * found that are associated with the particular activity specified.
	 * 
	 * @param act
	 * @return
	 */
	public static ArrayList<Activity> getWorkingPrinciples(Activity act, ArrayList<Activity> workingPrinciples) {
		ArrayList<Activity> wps = new ArrayList<Activity>();
		
		if(TransformationHelper.isWorkingPrinciple(act))
			wps.add(act);
		
		for(Activity wp : workingPrinciples) {
			// Go through inheritance tree to try and see whether one of the parents is "act"
			if (isInInheritanceHierarchy(act, wp))
				wps.add(wp);
		}
		
		return wps;
	}
	
	/**
	 * Returns a list of resources associated with a particular set of working principles
	 * that are associated with a given activity.
	 * <p>
	 * Note that the returned list contains <emph>all</emph> related resources, at any level of
	 * composition and whether abstract or not.
	 * 
	 * @param workingPrinciples The working principles as concrete implementations of the activity
	 * @return A list of resources that are specified as part of the working principles
	 */
	public static HashSet<Element> getResourceTypes(ArrayList<Activity> workingPrinciples) {
		HashSet<Element> res = new HashSet<Element>();
		
		for (Activity wp : workingPrinciples) {
			if (wp.getOwnedAttributes() != null) {
				for (Property p : wp.getOwnedAttributes()) {
					Type t = p.getType();
					
					if (TransformationHelper.isResource(t))
						res.add(t);
					
					// Get nested resource types, if any
					res.addAll(getAllNestedResourceTypes(t));
				}
			}
		}
		
		return res;
	}
	
	/**
	 * Returns a list of resources (potentially with duplicates) that are associated
	 * with a particular working principle.
	 * 
	 * @param workingPrinciple
	 * @return
	 */
	public static ArrayList<Element> getResourceTypesWithDuplicates(Activity workingPrinciple) {
		// TODO Shouldn't we rathre collect the properties, then algorithmically do other stuff
		//		such as composites / shareability
		ArrayList<Element> res = new ArrayList<Element>();
		
		if (workingPrinciple.getOwnedAttributes() != null) {
			for (Property p : workingPrinciple.getOwnedAttributes()) {
				Type t = p.getType();
				
				if (TransformationHelper.isResource(t))
					res.add(t);
				
				// Get nested resource types, if any
				res.addAll(getAllNestedResourceTypes(t));
			}
		}
		
		return res;
	}
	
	/**
	 * Retrieve nested resource types.
	 * <p>
	 * Performs a depth first search to identify resources at any level.
	 * 
	 * @param resource
	 * @return
	 */
	public static HashSet<Element> getAllNestedResourceTypes(Element resource) {
		HashSet<Element> parts = new HashSet<Element>();
		
		if (!TransformationHelper.isCompositeResource(resource))
			return parts;
		
		for (Element part : TransformationHelper.getResourceParts(resource)) {
			parts.add(part);
			parts.addAll(getAllNestedResourceTypes(part));
		}
		
		return parts;
	}
	
	/**
	 * Search the inheritance tree to find out whether a particular working principle
	 * is a concrete implementation of a particular activity.
	 * 
	 * @param act
	 * @param wp
	 * @return
	 */
	private static boolean isInInheritanceHierarchy(
			Activity act,
			Activity wp) {
		boolean isInHierarchy = false;
		
		// Go through generalization relationships and look for "act"
		if(wp.getGenerals() != null) {
			for(Classifier general : wp.getGenerals()) {
				if(general.equals(act))
					return true;
				
				// Recursively search the inheritance tree
				isInHierarchy |= isInInheritanceHierarchy(act, (Activity) general);
				
				if(isInHierarchy)
					return true;
			}
		}
		
		return isInHierarchy;
	}
	
	/**
	 * Rebuild the list of associations.
	 * 
	 * @return
	 */
	public static ArrayList<Association> rebuildAssociationList(Element rootElement) {
		ArrayList<Association> associations = new ArrayList<Association>();
		
		// Iterate through owned elements to find associations
		for(Element e : rootElement.getOwnedElements()) {
			// Depth first search
			if(e.getOwnedElements() != null)
				associations.addAll(rebuildAssociationList(e));
			
			if(e instanceof Association) {
				associations.add((Association) e);
			}
		}
		
		return associations;
	}
	
	/**
	 * Rebuild the list of object flows.
	 * 
	 * @return
	 */
	public static ArrayList<ObjectFlow> rebuildObjectFlowList(Element rootElement) {
		ArrayList<ObjectFlow> objectFlows = new ArrayList<ObjectFlow>();
		
		// Iterate through owned elements to find associations
		for(Element e : rootElement.getOwnedElements()) {
			// Depth first search
			if(e.getOwnedElements() != null)
				objectFlows.addAll(rebuildObjectFlowList(e));
			
			if(e instanceof ObjectFlow) {
				objectFlows.add((ObjectFlow) e);
			}
		}
		
		return objectFlows;
	}
	
	/**
	 * Check whether an Element is a machining resource.
	 * <p>
	 * Check whether an Element is a machining resource by going through the list of
	 * applied stereotypes, and checking for the "Resource" stereotype.
	 * 
	 * @param element
	 * @return true if the Element is a machining resource
	 */
	public static boolean oneBaseClassifierIsResource(Element e) {
		boolean isResource = false;
		
		if(e == null)
			return false;
		
		if(e instanceof Classifier && isAbstract((Classifier) e) && isResource(e))
			return true;
		
		if(((Classifier)e).getGenerals() == null)
			return false;
		
		// If any of the stereotypes applied to this element is "Resource", then 
		for(Classifier c : ((Classifier)e).getGenerals()) {
			isResource |= oneBaseClassifierIsResource(c);
			
			// Optimization: just return true instead of searching through rest
			if(isResource)
				return true;
		}
		
		return isResource;
	}
	
	/**
	 * @param element
	 * @return true if the Element is a machining resource
	 */
	public static boolean oneBaseClassifierIsSpecificResource(Element e, Classifier resource) {
		boolean isResource = false;
		
		if (e == null)
			return false;
		
		if (e == resource)
			return true;
		
		if (((Classifier)e).getGenerals() == null)
			return false;
		
		// If any of the stereotypes applied to this element is "Resource", then 
		for (Classifier c : ((Classifier)e).getGenerals()) {
			isResource |= oneBaseClassifierIsSpecificResource(c, resource);
			
			// Optimization: just return true instead of searching through rest
			if(isResource)
				return true;
		}
		
		return isResource;
	}
	
	/**
	 * Check whether an Element is a system under design.
	 * 
	 * @param element
	 * @return true if the Element is a system under design
	 */
	public static boolean isSystemUnderDesign(Element e) {
		if (e == null)
			return false;
		
		return isStereotypeApplied(e, "SystemUnderDesign");
	}
	
	/**
	 * Checks whether a stereotype of a given name is applied to an element.
	 * 
	 * @param e
	 * @param stereotypeName
	 * @return
	 */
	public static boolean isStereotypeApplied(Element e, String stereotypeName) {
		if (e == null)
			return false;
		
		for (Stereotype s : e.getAppliedStereotypes())
			if (s.getName().equals(stereotypeName))
				return true;
		
		return false;
	}
	
	/**
	 * Check whether an Element is a machining resource.
	 * <p>
	 * Check whether an Element is a machining resource by going through the list of
	 * applied sterotypes, and checking for the "Resource" sterotype.
	 * 
	 * @param element
	 * @return true if the Element is a machining resource
	 */
	public static boolean isResource(Element e) {
		if (e == null)
			return false;
		
		return (isStereotypeApplied(e, "Resource") || isStereotypeApplied(e, "Machine"));
	}
	
	/**
	 * Checks whether the given element is stereotyped with
	 * "WorkingPrinciple".
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isWorkingPrinciple(Element e) {
		if (e == null)
			return false;
		
		// Updated profile: working principles have to be activities (by definition)
		if (!(e instanceof Activity))
			return false;
		
		return isStereotypeApplied(e, "WorkingPrinciple");
	}
	
	/**
	 * Checks whether the given element is stereotyped with
	 * "WorkingPrinciple" at some level of inheritance.
	 * <P>
	 * Note: this function searches from general to specific
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isWorkingPrincipleAtSomeLevel(Element e, ArrayList<Activity> workingPrinciples) {
		boolean isWorkingPrinciple = false;
		
		if (e == null)
			return false;
		
		// Cannot be a working principle if not an activity
		if (!(e instanceof Activity))
			return false;
		
		if (isWorkingPrinciple(e))
			return true;
		
		// Check inheritance hierarchy
		if (getWorkingPrinciples((Activity) e, workingPrinciples).size() > 0)
			return true;
		
		return isWorkingPrinciple;
	}
	
	/**
	 * Checks whether the given element is a workpiece.
	 * 
	 * @param e
	 * @return
	 */
	public static boolean isWorkpiece(Element e) {
		if (e == null)
			return false;
		
		// Cannot be a workpiece if not a classifier
		if (!(e instanceof Classifier))
			return false;
		
		// FIXME Done based on name right now
		if (((Classifier) e).getName().equals("Workpiece"))
			return true;
		
		return false;
	}
	
	/** Wrapper function for {@link Classifier#isAbstract()}. */
	public static boolean isAbstract(Classifier c) {
		return c.isAbstract();
	}
	
	/**
	 * Checks whether a particular resource is a composite resource.
	 * <P>
	 * This function returns true or false depending on whether the specified
	 * resource is a composite resource (e.g., a robot with a gripper).
	 * 
	 * @param e
	 * @return
	 */
	public static boolean isCompositeResource(Element e) {
		if (!isResource(e))
			return false;
		
		if (!getResourceParts(e).isEmpty())
			return true;
		
		return false;
	}
	
	/**
	 * Check whether a Constraint object is an objective
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isObjective(Constraint c) {
		if(c == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		return isStereotypeApplied(c, "Objective");
	}
	
	/**
	 * Check whether a particular property is a duration property
	 * 
	 * @param p
	 * @return
	 */
	public static boolean isDurationProperty(Property p) {
		if(p == null)
			return false;
		
		// If any of the stereotypes applied to this element is of type modelcenter data model, return true
		return isStereotypeApplied(p, "processDuration");
	}
	
	/**
	 * Returns the composite parts of a resource, if any.
	 * <P>
	 * This function returns all of the composite parts of a composite resource.
	 * If the resource is not a composite resource, an empty arraylist is returned.
	 * 
	 * @param e
	 * @return
	 */
	public static ArrayList<Element> getResourceParts(Element e) {
		ArrayList<Element> composites = new ArrayList<Element>();
		
		// FIXME The way redefined properties are treated still seems a little shaky
		ArrayList<Element> ownedAndInheritedProperties = new ArrayList<Element>();
		ownedAndInheritedProperties.addAll(e.getOwnedElements());
		ownedAndInheritedProperties.addAll(((Classifier) e).getInheritedMembers());
		
		// Skip redefined properties in inherited
		ArrayList<Property> skip = new ArrayList<Property>();
		
		if (e.getOwnedElements() != null) {
			for (Element o : ownedAndInheritedProperties) {
				if (o instanceof Property
						&& isResource(((Property) o).getType())
						&& !skip.contains(o)) {		// Skip properties that have been redefined
					composites.add(((Property) o).getType());
					
					if (((Property) o).getRedefinedProperties() != null
							&& !((Property) o).getRedefinedProperties().isEmpty()) {
						for (Property toSkip : ((Property) o).getRedefinedProperties()) {
							skip.add(toSkip);
						}
					}
				}
			}
		}
		
		return composites;
	}
	
	/**
	 * Checks whether the given property is a SysML value property.
	 * 
	 * @param p
	 * @return
	 */
	public static boolean isSysMLValueProperty(Element p) {
		if (p == null)
			return false;
		
		// Note: this is NOT the case in Papyrus, where there is NO stereotype "valueProperty" defined!!
		//return isStereotypeApplied(p, "ValueProperty");
		return (p instanceof Property);
	}
	
}
