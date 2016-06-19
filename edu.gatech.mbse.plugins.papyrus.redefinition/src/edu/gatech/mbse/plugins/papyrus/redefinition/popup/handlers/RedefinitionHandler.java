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
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.StructuredClassifier;

import edu.gatech.mbse.plugins.papyrus.redefinition.popup.states.CommandState;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;

/**
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RedefinitionHandler extends AbstractHandler {

	/** Log4J object. */
	private static final Logger logger = LogManager.getLogger(RedefinitionHandler.class.getName());
	
	/**
	 * The constructor.
	 */
	public RedefinitionHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISourceProviderService service = (ISourceProviderService) PlatformUI.getWorkbench().getService(  
                ISourceProviderService.class);
		
        ISourceProvider provider = service.getSourceProvider(CommandState.PROVIDER_NAME);
        
		// Enable if at least one of the selected objects is a Classifier instance
		List<NamedElement> selectedObjects = getSelectedUmlObjects();
		//List<Object> selectedEObjects = lookupSelectedElements();

		for (NamedElement e : selectedObjects) {
			if (e instanceof Classifier) {
				final TransactionalEditingDomain ted = TransactionUtil.getEditingDomain(e); // (TransactionalEditingDomain) AdapterFactoryEditingDomain.getEditingDomainFor(e);
				final Classifier theElement = (Classifier) e;
				
				ted.getCommandStack().execute(new RecordingCommand(ted, "Redefine Inherited Value Properties") {

					@Override
					protected void doExecute() {
						redefineInheritedValueProperties(theElement);
					}
					
				});
			}
		}
        
		return null;
	}
	
	/**
	 * Returns the selected elements.
	 * <p>
	 * Credit to https://wiki.eclipse.org/Papyrus_Developer_Guide/How_To_Code_Examples#Core_Examples
	 * 
	 * @return
	 */
	protected List<Object> lookupSelectedElements() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		ISelection selection = page.getSelection();
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			return structuredSelection.toList();
		} else if (selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection) selection;
			return treeSelection.toList();
		}
		return null;
	}
	
	/**
	 * Return a list of selected domain (UML) elements.
	 * <p>
	 * Credit to https://wiki.eclipse.org/Papyrus_Developer_Guide/How_To_Code_Examples#Core_Examples
	 * 
	 * @return
	 */
	protected List<NamedElement> getSelectedUmlObjects() {
		List<Object> selections = lookupSelectedElements(); // see "How to Get
															// the Current
															// Selection from
															// Java code"

		List<NamedElement> results = new ArrayList<NamedElement>();

		// create model with EList<EObject> objects
		for (Object obj : selections) {
			// Adapt object to NamedElement
			NamedElement ele = null;
			if (obj instanceof IAdaptable) {
				ele = (NamedElement) ((IAdaptable) obj).getAdapter(NamedElement.class);
			}
			if (ele == null) {
				ele = (NamedElement) Platform.getAdapterManager().getAdapter(obj, NamedElement.class);
			}
			if (ele != null) {
				results.add(ele);
			}
		}
		return results;
	}
	
	protected NamedElement toNamedElement(Object obj) {
		// Adapt object to NamedElement
		NamedElement ele = null;
		
		if (obj instanceof IAdaptable) {
			ele = (NamedElement) ((IAdaptable) obj).getAdapter(NamedElement.class);
		}
		if (ele == null) {
			ele = (NamedElement) Platform.getAdapterManager().getAdapter(obj, NamedElement.class);
		}
		
		return ele;
	}
	
	/**
	 * Adds and redefines value properties.
	 * <P>
	 * Walks through the inheritance tree and adds and redefines all owned and inherited
	 * value properties.
	 * 
	 * @param c The classifier to redefine properties in
	 */
	public void redefineInheritedValueProperties(Classifier c) {
		// Collect all inherited properties, redefine
		for(NamedElement e : c.getInheritedMembers()) {
			logger.trace("Inherited member is: " + e.getName());
			
			// Check whether element is a value property
			if(e instanceof Property
					&& TransformationHelper.isSysMLValueProperty(e)
					&& isNotOwnedProperty(e, c)) {
				// Get elements factory
				//ElementsFactory elementsFactory = Application.getInstance().getProject().getElementsFactory();
				
				// Property instance
				//Property newProperty = elementsFactory.createPropertyInstance();
				Property newProperty = ((StructuredClassifier) c).createOwnedAttribute(e.getName(), ((Property) e).getType());
				
				for (Stereotype s : ((Property) e).getAppliedStereotypes())
					newProperty.applyStereotype(s);
				//StereotypesHelper.addStereotype(newProperty, MDSysMLModelHandler.getStereotypeSysMLValueProperty());
				
				// Name & type
				newProperty.setName(e.getName());
				newProperty.setType(((Property) e).getType());
				
				// TODO Multiplicity, ...
				if (((Property) e).getUpperValue() != null) {
					newProperty.setUpperValue(((Property) e).getUpperValue());
				}
				
				if (((Property) e).getLowerValue() != null) {
					newProperty.setLowerValue(((Property) e).getLowerValue());
				}
				
				// Add to parent
				//c.getAttributes().add(newProperty);
				
				// Set redefinition context
				//newProperty.getRedefinitionContexts().add(c);
				newProperty.getRedefinedProperties().add((Property) e);
				//if(!newProperty.getRedefinedElements().contains(e))
				//	newProperty.getRedefinedElements().add((RedefinableElement) e);
				
				// Visibility: private, public or protected
				newProperty.setVisibility(((Property) e).getVisibility());
				
				// Set aggregation kind (leads to Papyrus validation error otherwise!)
				newProperty.setAggregation(((Property) e).getAggregation());
			}
		}
	}

	/**
	 * Checks whether a given property (Element p) is owned by c. This is done by
	 * checking whether there is an owned property that redefines the given property.
	 * 
	 * @param e
	 * @param c
	 * @return
	 */
	private boolean isNotOwnedProperty(NamedElement p, Classifier c) {
		for(NamedElement e : c.getOwnedMembers()) {
			if (e instanceof Property
					&& ((Property) e).getRedefinitionContexts().contains(c)
					&& ((Property) e).getRedefinedProperties().contains(p))
				return false;
		}
		
		return true;
	}
	
}
