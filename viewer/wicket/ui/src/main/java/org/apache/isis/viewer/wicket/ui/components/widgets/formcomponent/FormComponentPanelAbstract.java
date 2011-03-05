/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.isis.viewer.wicket.ui.components.widgets.formcomponent;

import java.util.List;

import org.apache.isis.core.commons.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.oid.stringable.OidStringifier;
import org.apache.isis.runtimes.dflt.runtime.context.IsisContext;
import org.apache.isis.runtimes.dflt.runtime.persistence.PersistenceSession;
import org.apache.isis.viewer.wicket.model.nof.AuthenticationSessionAccessor;
import org.apache.isis.viewer.wicket.model.nof.PersistenceSessionAccessor;
import org.apache.isis.viewer.wicket.ui.ComponentType;
import org.apache.isis.viewer.wicket.ui.app.imagecache.ImageCache;
import org.apache.isis.viewer.wicket.ui.app.imagecache.ImageCacheAccessor;
import org.apache.isis.viewer.wicket.ui.app.registry.ComponentFactoryRegistry;
import org.apache.isis.viewer.wicket.ui.app.registry.ComponentFactoryRegistryAccessor;
import org.apache.isis.viewer.wicket.ui.pages.PageClassRegistry;
import org.apache.isis.viewer.wicket.ui.pages.PageClassRegistryAccessor;
import org.apache.isis.viewer.wicket.ui.util.Components;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;

/**
 * Convenience adapter for {@link FormComponent}s 
 * that are implemented using the Wicket {@link FormComponentPanel}, providing
 * the ability to build up the panel using other {@link ComponentType}s.
 */
public abstract class FormComponentPanelAbstract<T> extends FormComponentPanel<T> implements PersistenceSessionAccessor, AuthenticationSessionAccessor {

	private static final long serialVersionUID = 1L;

	private ComponentType componentType;

	public FormComponentPanelAbstract(ComponentType componentType) {
		this(componentType, null);
	}

	public FormComponentPanelAbstract(String id) {
		this(id, null);
	}

	public FormComponentPanelAbstract(ComponentType componentType, IModel<T> model) {
		this(componentType.getWicketId(), model);
	}

	public FormComponentPanelAbstract(String id, IModel<T> model) {
		super(id, model);
		this.componentType = ComponentType.lookup(id);
	}


	public ComponentType getComponentType() {
		return componentType;
	}
	
	
	/**
	 * For subclasses
	 * @return 
	 */
	protected Component addOrReplace(ComponentType componentType, IModel<?> model) {
		return getComponentFactoryRegistry().addOrReplaceComponent(this, componentType, model);
	}

	/**
	 * For subclasses
	 */
	protected void permanentlyHide(ComponentType... componentIds) {
		permanentlyHide(this, componentIds);
	}

	/**
	 * For subclasses
	 */
	public void permanentlyHide(String... ids) {
		permanentlyHide(this, ids);
	}

	/**
	 * For subclasses
	 */
	protected void permanentlyHide(MarkupContainer container, ComponentType... componentIds) {
		Components.permanentlyHide(container, componentIds);
	}

	/**
	 * For subclasses
	 */
	public void permanentlyHide(MarkupContainer container, String... ids) {
		Components.permanentlyHide(container, ids);
	}


	/////////////////////////////////////////////////////////////////////
	// Convenience
	/////////////////////////////////////////////////////////////////////

	protected ComponentFactoryRegistry getComponentFactoryRegistry() {
		final ComponentFactoryRegistryAccessor cfra = (ComponentFactoryRegistryAccessor)getApplication();
        return cfra.getComponentFactoryRegistry();
	}

    protected PageClassRegistry getPageClassRegistry() {
        final PageClassRegistryAccessor pcra = (PageClassRegistryAccessor) getApplication();
        return pcra.getPageClassRegistry();
    }

    protected ImageCache getImageCache() {
        final ImageCacheAccessor ica = (ImageCacheAccessor)getApplication();
        return ica.getImageCache();
    }



	/**
	 * The underlying {@link AuthenticationSession Isis session} wrapped in the
	 * {@link #getWebSession() Wicket session}.
	 * @return
	 */
	public AuthenticationSession getAuthenticationSession() {
		return ((AuthenticationSessionAccessor) Session.get()).getAuthenticationSession();
	}

	/////////////////////////////////////////////////////////////////////
	// Dependencies (from IsisContext)
	/////////////////////////////////////////////////////////////////////
	
	public PersistenceSession getPersistenceSession() {
		return IsisContext.getPersistenceSession();
	}
	
	protected List<ObjectAdapter> getServiceAdapters() {
		return getPersistenceSession().getServices();
	}

	protected OidStringifier getOidStringifier() {
		return getPersistenceSession().getOidGenerator().getOidStringifier();
	}

	
}
