/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.jvm.Jvm;
import org.jboss.as.console.client.shared.jvm.JvmEditor;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 3/3/11
 */
public class ServerConfigView extends SuspendableViewImpl implements ServerConfigPresenter.MyView {

    private ServerConfigPresenter presenter;

    private ServerConfigDetails details;
    private JvmEditor jvmEditor;
    private PropertyEditor propertyEditor;

    private ContentHeaderLabel headline;
    private SplitLayoutPanel splitlayout;
    private FinderColumn<Server> serverColumn;


    interface ServerTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}&nbsp;<span style='font-size:8px'>({2})</span></div>")
        SafeHtml item(String cssClass, String server, String host);
    }

    private static final ServerTemplate SERVER_TEMPLATE = GWT.create(ServerTemplate.class);

    public ServerConfigView() {

    }

    @Override
    public void setPresenter(ServerConfigPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {

        final ToolStrip toolStrip = new ToolStrip();

        /*ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler(){
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchNewConfigDialoge();
            }
        });
        addBtn.setOperationAddress("/{selected.host}/server-config=*", "add");
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_serverConfigView());
        toolStrip.addToolButtonRight(addBtn);*/

       /* ToolButton deleteBtn = new ToolButton(Console.CONSTANTS.common_label_delete());
        deleteBtn.addClickHandler(new ClickHandler(){
            @Override
            public void onClick(ClickEvent clickEvent) {

                final Server server = serverColumn.getSelectedItem();

                Feedback.confirm(
                        Console.MESSAGES.deleteServerConfig(),
                        Console.MESSAGES.deleteServerConfigConfirm(server.getName()),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed)
                                    presenter.tryDelete(server);
                            }
                        });
            }
        });
        deleteBtn.setOperationAddress("/{selected.host}/server-config=*", "remove");
        deleteBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_delete_serverConfigView());
        toolStrip.addToolButtonRight(deleteBtn);*/


      /*  ToolButton copyBtn = new ToolButton(Console.CONSTANTS.common_label_copy());
        copyBtn.addClickHandler(new ClickHandler(){
            @Override
            public void onClick(ClickEvent clickEvent) {

                final Server server = serverColumn.getSelectedItem();
                presenter.onLaunchCopyWizard(server);
            }
        });
        copyBtn.setOperationAddress("/{selected.host}/server-config=*", "add");

        toolStrip.addToolButtonRight(copyBtn);
        toolStrip.setFilter("/{selected.host}/server-config=*");  */

        // ------------------------------------------------------


        details = new ServerConfigDetails(presenter);

        // jvm editor
        jvmEditor = new JvmEditor(presenter, true, true);
        jvmEditor.setAddressCallback(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {

                ModelNode address = new ModelNode();
                address.add("host", presenter.getSelectedHost());
                address.add("server-config", "*");
                address.add("jvm", "*");
                return address;
            }
        });

        propertyEditor = new PropertyEditor(presenter, true);
//        propertyEditor.setOperationAddress("/{selected.host}/server-config=*/system-property=*", "add");


        // --------------------

        headline = new ContentHeaderLabel();

        OneToOneLayout editor = new OneToOneLayout()
                .setTitle("Server Configuration")
                .setHeadlineWidget(headline)
                .setDescription(Console.CONSTANTS.common_serverConfig_desc())
                .addDetail("Attributes", details.asWidget())
                .addDetail(Console.CONSTANTS.common_label_virtualMachine(), jvmEditor.asWidget())
                .addDetail(Console.CONSTANTS.common_label_systemProperties(), propertyEditor.asWidget());
        // 1. Filter must be set *after* jvmEditor.asWidget()
        // 2. We don't get exceptions for nested resources like "/{selected.host}/server-config=*/jvm=*",
        // so we use the parent address assuming that the privileges are the same - i.e. if we cannot modify the
        // server-config, we shouldn't be able to edit the JVM settings either.
        jvmEditor.setSecurityContextFilter("/{selected.host}/server-config=*");

        splitlayout = new SplitLayoutPanel(2);

        serverColumn = new FinderColumn<Server>(
                "Server",
                new FinderColumn.Display<Server>() {

                    @Override
                    public boolean isFolder(Server data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, Server data) {
                        String context = presenter.getFilter().equals(FilterType.HOST)  ? data.getGroup() : data.getHostName();
                        return SERVER_TEMPLATE.item(baseCss, data.getName(), context);
                    }
                },
                new ProvidesKey<Server>() {
                    @Override
                    public Object getKey(Server item) {
                        return item.getName()+item.getHostName();
                    }
                })
                .setMenuItems(
                        new MenuDelegate<Server>(          // TODO permissions
                                "Remove", new ContextualCommand<Server>() {
                            @Override
                            public void executeOn(final Server server) {

                                Feedback.confirm(
                                        Console.MESSAGES.deleteServerConfig(),
                                        Console.MESSAGES.deleteServerConfigConfirm(server.getName()),
                                        new Feedback.ConfirmationHandler() {
                                            @Override
                                            public void onConfirmation(boolean isConfirmed) {
                                                if (isConfirmed)
                                                    presenter.tryDelete(server);
                                            }
                                        });
                            }
                        }),
                        new MenuDelegate<Server>(
                                "Copy", new ContextualCommand<Server>() {
                            @Override
                            public void executeOn(Server server) {
                                presenter.onLaunchCopyWizard(server);
                            }
                        })
                );


        serverColumn.setTopMenuItems(
                new MenuDelegate<Server>(
                        "<i class=\"icon-plus\" style='color:black'></i>&nbsp;New", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {
                        presenter.launchNewConfigDialoge();
                    }
                })
        );

        splitlayout.addWest(serverColumn.asWidget(), 217);
        splitlayout.add(editor.build());


        serverColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (serverColumn.hasSelectedItem()) {

                    final Server selectedServer = serverColumn.getSelectedItem();

                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        public void execute() {

                            Console.getCircuit().dispatch(
                                    new SelectServer(selectedServer.getHostName(), selectedServer.getName())
                            );
                        }
                    });
                }
            }
        });

        return splitlayout;
    }


    @Override
    public void updateServerList(List<Server> serverModel) {
        serverColumn.updateFrom(serverModel, true);
    }


    @Override
    public void setJvm(String reference, Jvm jvm) {
        jvmEditor.setSelectedRecord(reference, jvm);
    }

    @Override
    public void updateSocketBindings(List<String> result) {
        if(details!=null)
            details.setAvailableSockets(result);
    }

    @Override
    public void setProperties(String reference, List<PropertyRecord> properties) {
        propertyEditor.setProperties(reference, properties);
    }

    @Override
    public void updateFrom(Server server) {
        headline.setHTML("Server '"+server.getName()+"' on Host '"+server.getHostName()+"'");

        details.clearValues();
        jvmEditor.clearValues();
        propertyEditor.clearValues();

        details.updateFrom(server);

        // lazily fetch jvm and property settings
        presenter.onServerConfigSelectionChanged(server);

    }

    @Override
    public void setGroups(List<ServerGroupRecord> result) {
        details.setAvailableGroups(result);
    }

}
