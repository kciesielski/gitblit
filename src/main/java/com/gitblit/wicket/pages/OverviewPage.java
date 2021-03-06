/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.wicket.pages;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.jgit.lib.Repository;

import com.gitblit.GitBlit;
import com.gitblit.Keys;
import com.gitblit.models.Metric;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.UserModel;
import com.gitblit.utils.JGitUtils;
import com.gitblit.wicket.GitBlitWebSession;
import com.gitblit.wicket.WicketUtils;
import com.gitblit.wicket.charting.GoogleChart;
import com.gitblit.wicket.charting.GoogleCharts;
import com.gitblit.wicket.charting.GoogleLineChart;
import com.gitblit.wicket.panels.BranchesPanel;
import com.gitblit.wicket.panels.LinkPanel;
import com.gitblit.wicket.panels.PushesPanel;
import com.gitblit.wicket.panels.RepositoryUrlPanel;
import com.gitblit.wicket.panels.TagsPanel;

public class OverviewPage extends RepositoryPage {

	public OverviewPage(PageParameters params) {
		super(params);

		int numberRefs = GitBlit.getInteger(Keys.web.summaryRefsCount, 5);

		Repository r = getRepository();
		final RepositoryModel model = getRepositoryModel();
		UserModel user = GitBlitWebSession.get().getUser();
		if (user == null) {
			user = UserModel.ANONYMOUS;
		}

		List<Metric> metrics = null;
		Metric metricsTotal = null;
		if (!model.skipSummaryMetrics && GitBlit.getBoolean(Keys.web.generateActivityGraph, true)) {
			metrics = GitBlit.self().getRepositoryDefaultMetrics(model, r);
			metricsTotal = metrics.remove(0);
		}

		addSyndicationDiscoveryLink();

		// repository description
		add(new Label("repositoryDescription", getRepositoryModel().description));
		
		// owner links
		final List<String> owners = new ArrayList<String>(getRepositoryModel().owners);
		ListDataProvider<String> ownersDp = new ListDataProvider<String>(owners);
		DataView<String> ownersView = new DataView<String>("repositoryOwners", ownersDp) {
			private static final long serialVersionUID = 1L;
			int counter = 0;
			public void populateItem(final Item<String> item) {
				UserModel ownerModel = GitBlit.self().getUserModel(item.getModelObject());
				if (ownerModel != null) {
					item.add(new LinkPanel("owner", null, ownerModel.getDisplayName(), UserPage.class,
							WicketUtils.newUsernameParameter(ownerModel.username)).setRenderBodyOnly(true));
				} else {
					item.add(new Label("owner").setVisible(false));
				}
				counter++;
				item.add(new Label("comma", ",").setVisible(counter < owners.size()));
				item.setRenderBodyOnly(true);
			}
		};
		ownersView.setRenderBodyOnly(true);
		add(ownersView);
		
		add(WicketUtils.createTimestampLabel("repositoryLastChange",
				JGitUtils.getLastChange(r), getTimeZone(), getTimeUtils()));
		add(new Label("repositorySize", model.size));
		
		if (metricsTotal == null) {
			add(new Label("branchStats", ""));
		} else {
			add(new Label("branchStats",
					MessageFormat.format(getString("gb.branchStats"), metricsTotal.count,
							metricsTotal.tag, getTimeUtils().duration(metricsTotal.duration))));
		}
		add(new BookmarkablePageLink<Void>("metrics", MetricsPage.class,
				WicketUtils.newRepositoryParameter(repositoryName)));

		add(new RepositoryUrlPanel("repositoryUrlPanel", false, user, model));

		PushesPanel pushes = new PushesPanel("pushesPanel", getRepositoryModel(), r, 10, 0);
		add(pushes);
		add(new TagsPanel("tagsPanel", repositoryName, r, numberRefs).hideIfEmpty());
		add(new BranchesPanel("branchesPanel", getRepositoryModel(), r, numberRefs, false).hideIfEmpty());

		// Display an activity line graph
		insertActivityGraph(metrics);
	}

	@Override
	protected String getPageName() {
		return getString("gb.overview");
	}

	private void insertActivityGraph(List<Metric> metrics) {
		if ((metrics != null) && (metrics.size() > 0)
				&& GitBlit.getBoolean(Keys.web.generateActivityGraph, true)) {
			
			// daily line chart
			GoogleChart chart = new GoogleLineChart("chartDaily", "", "unit",
					getString("gb.commits"));
			for (Metric metric : metrics) {
				chart.addValue(metric.name, metric.count);
			}
			chart.setWidth(375);
			chart.setHeight(150);
			
			GoogleCharts charts = new GoogleCharts();
			charts.addChart(chart);
			add(new HeaderContributor(charts));
		}
	}
}
