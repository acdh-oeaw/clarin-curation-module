package eu.clarin.web.components;

import java.text.NumberFormat;

import com.vaadin.v7.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.v7.ui.Grid;
import com.vaadin.v7.ui.Grid.CellReference;
import com.vaadin.v7.ui.Grid.CellStyleGenerator;
import com.vaadin.v7.ui.Grid.HeaderCell;
import com.vaadin.v7.ui.Grid.HeaderRow;
import com.vaadin.v7.ui.Grid.SelectionMode;

import eu.clarin.web.MainUI;
import eu.clarin.web.Shared;

import com.vaadin.ui.Panel;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;

public abstract class GridPanel extends Panel implements View {
	
	private static final long serialVersionUID = -7374530411450324804L;
	
	protected VerticalLayout sideMenu;
	protected Grid grid;
	
	protected static final NumberFormat PERCENTAGE = NumberFormat.getPercentInstance();
	
	static {
        PERCENTAGE.setMaximumFractionDigits(1);
        PERCENTAGE.setMinimumFractionDigits(1);
    }

	public GridPanel() {
		this.setSizeFull();
		
		sideMenu = new VerticalLayout();

		VerticalLayout layout = new VerticalLayout(generateGrid());
		layout.setSizeFull();
		layout.setSpacing(true);

		this.setContent(layout);
	}	
	
	@Override
	public void enter(ViewChangeEvent event) {
		((MainUI)getUI()).setCustomMenu(sideMenu);
	}

	private Grid generateGrid() {
		IndexedContainer container = createContainer();
		grid = new Grid(container);
		
		grid.setCellStyleGenerator(cell -> {
		    if(cell.getProperty().getType().isAssignableFrom(Boolean.class)) {
		        if (Shared.facetNames.contains(cell.getPropertyId())) {
	                return (boolean) cell.getValue() ? "facetCovered" : "facetNotCovered";
		        }
		    }
		    else if(cell.getProperty().getType().getSuperclass().equals(Number.class))
                return "align-right";                
            return null;
        });
		grid.setSizeFull();

		grid.setSelectionMode(SelectionMode.NONE);

		// set filtering
		HeaderRow filterRow = grid.appendHeaderRow();

		grid.getContainerDataSource().getContainerPropertyIds().forEach(pid -> {
			HeaderCell cell = filterRow.getCell(pid);
			TextField filterField = new TextField();
			filterField.setInputPrompt("Filter...");
			filterField.setStyleName("filter");
			filterField.setColumns(0);			

			filterField.addTextChangeListener(change -> {
				container.removeContainerFilters(pid);

				if (!change.getText().isEmpty())
					container.addContainerFilter(pid, change.getText(), true, false);
			});
			cell.setComponent(filterField);
		});
		
		//custom rendering for columns
		customRendering();
		
		fillInData();
		LinkButton export = new LinkButton("Export as TSV");		
		sideMenu.addComponent(export);
		
		FileDownloader fileDownloader = new FileDownloader(generateStreamResource());		
		fileDownloader.extend(export);

		return grid;
	}

	protected abstract IndexedContainer createContainer();
	
	protected abstract void customRendering();
	
	protected abstract void fillInData();
	
	protected abstract StreamResource generateStreamResource();

}
