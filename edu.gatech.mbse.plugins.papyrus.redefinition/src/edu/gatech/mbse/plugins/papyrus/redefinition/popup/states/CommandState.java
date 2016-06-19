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
package edu.gatech.mbse.plugins.papyrus.redefinition.popup.states;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;

/**
 * @author Sebastian
 *
 */
public class CommandState extends AbstractSourceProvider {

	public final static String PROVIDER_NAME = "edu.gatech.mbse.plugins.papyrus.redefinition.popup.states";

	private boolean enabled = false;
	
	/**
	 * Constructor.
	 *
	 */
	public CommandState() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see org.eclipse.ui.ISourceProvider#dispose()
	 *
	 */
	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.eclipse.ui.ISourceProvider#getCurrentState()
	 *
	 * @return
	 */
	@Override
	public Map getCurrentState() {
		Map map = new HashMap(1);
		
	    String value = enabled ? "enabled" : "notEnabled";
	    
	    map.put(PROVIDER_NAME, value);
	    
	    return map;
	}

	/**
	 * @see org.eclipse.ui.ISourceProvider#getProvidedSourceNames()
	 *
	 * @return
	 */
	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { PROVIDER_NAME };
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
