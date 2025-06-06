package org.dspace.app.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.FilteredItemConverter;
import org.dspace.app.rest.model.FilteredItemRest;
import org.dspace.app.rest.model.FilteredItemsRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.FilteredItemsResource;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.contentreport.FilteredItems;
import org.dspace.contentreport.QueryOperator;
import org.dspace.contentreport.QueryPredicate;
import org.dspace.contentreport.service.ContentReportService;
import org.dspace.core.Context;
import org.dspace.discovery.*;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/" + RestModel.REF_REPORT)
public class RefReportRestController implements InitializingBean {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;
    @Autowired
    private ConverterService converter;
    @Autowired
    private FilteredItemConverter itemConverter;
    @Autowired
    private ContentReportService contentReportService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private MetadataFieldService metadataFieldService;
    @Autowired
    private MetadataSchemaService metadataSchemaService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private SearchService searchService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/refitems")
    public ResponseEntity<RepresentationModel<?>> getRefItems(@RequestParam(name = "field", required = false) String field,
                                  @RequestParam(name = "author", required = false) String author,
                                  @RequestParam(name = "startDate", required = false) String startDateString,
                                  @RequestParam(name = "endDate", required = false) String endDateString,
                                  HttpServletRequest request, HttpServletResponse response) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        log.info("Start of RefReport");
        log.info("getRefItems field: '{}' author: '{}' startDate: '{}', endDate: '{}'", field, author, startDateString, endDateString);

        //log.info("Dates 'created'");
        int rows = 0;
        FilteredItems report = new FilteredItems();
        List<Item> filteredItems = new ArrayList<>();
        try {
            MetadataField mdf;
            Iterator<Item> items = null;
            if (StringUtils.isNotEmpty(field)) {
                log.info("Get the items via field: {}", field);
                mdf = metadataFieldService.find(context, Integer.parseInt(field));
                items = itemService.findByMetadataField(context, mdf.getMetadataSchema().getName(), mdf.getElement(), mdf.getQualifier(), Item.ANY);
            } if (StringUtils.isNotEmpty(author)) {
                log.info("Get the items for Author metadata {}", author);
                items = this.getAuthorItems(context, author);
            } if (StringUtils.isNotEmpty(endDateString)) {
                log.info("Get the items for endDate metadata {}", endDateString);
                String query = "refterms.dateAccepted_dt:[* TO " + endDateString + "T00:00:00Z" + "]";
                //String query = "dc.date.accessioned_dt:[* TO " + endDateString + "T00:00:00Z" + "]";
                log.info("Query {}", query);
                items = this.getDateItems(context, query);
            } if (StringUtils.isNotEmpty(startDateString)) {
                log.info("Get the items for startDate metadata {}", startDateString);
                String query = "refterms.dateAccepted_dt:[" + startDateString + "T00:00:00Z" + " TO *]";
                //String query = "dc.date.accessioned_dt:[" + startDateString + "T00:00:00Z" + " TO *]";
                log.info("Query {}", query);
                items = this.getDateItems(context, query);
            }

            log.info("Parse the items");
            items.forEachRemaining(filteredItems::add);
            /*while (items.hasNext()) {
                log.info("Start Parsing the items: ");
                Item dspaceItem = items.next();
                log.info("Parsing the item: {} ", dspaceItem.getName());

                if (StringUtils.isNotEmpty(field)) {
                    log.info("Adding field Item: " + dspaceItem.getName());
                    filteredItems.add(dspaceItem);
                    log.info("Added field Item");
                    rows += 1;
                } else if (StringUtils.isNotEmpty(author)) {
                    log.info("Adding Author Item: " + dspaceItem.getName());
                    filteredItems.add(dspaceItem);
                    log.info("Added Author Item");
                    rows += 1;
                } else if (StringUtils.isNotEmpty(endDateString)) {
                    log.info("Adding EndDateString Item: " + dspaceItem.getName());
                    filteredItems.add(dspaceItem);
                    log.info("Added EndDateString Item");
                    rows += 1;
                } else if (StringUtils.isNotEmpty(startDateString)) {
                    log.info("Adding StartDateString Item: " + dspaceItem.getName());
                    filteredItems.add(dspaceItem);
                    log.info("Added StartDateString Item");
                    rows += 1;
                } //else {
                    //if (checkItem(dspaceItem, author, startDate, endDate)) {
                    //    log.info("Adding Item: " + dspaceItem.getName());
                    //    filteredItems.add(dspaceItem);
                    //    log.info("Added Item");
                    //    rows += 1;

                    //}
                //}
            }*/

            log.info("We have found {} items", rows);
            log.info("No of filtered Items found: {}", filteredItems.size());
            log.info("Add items to report");

            filteredItems.stream()
                    .forEach(report::addItem);
            report.setItemCount(rows);

            log.info("Create the filtered items rest");
            log.info("No of items in report found1: {}", report.getItemCount());
            log.info("No of items in report found2: {}", report.getItems().size());
            List<FilteredItemRest> filteredItemsRest = report.getItems().stream()
                    .map(item -> itemConverter.convert(item, Projection.DEFAULT))
                    .collect(Collectors.toList());
            log.info("Create the rest report");
            FilteredItemsRest restReport = FilteredItemsRest.of(filteredItemsRest, report.getItemCount());
            restReport.setId("reftems");

            log.info("Convert rest report to resource");
            FilteredItemsResource result = converter.toResource(restReport);
            log.info("End of RefReport");
            return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), result);

        } catch (AuthorizeException authExc) {
            log.error("Filter check on item failed because of lacking authorization.\n" + authExc.toString());
        }
		catch (IOException ioExc)	{
            log.error("Filter check on item failed because IO.\n" + ioExc.toString());
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    /*private boolean checkItem(Item refItem, String author, Date startDate, Date stopDate)	{
        String refAuthor = getMetadata(itemService.getMetadata(refItem, "dc", "contributor","author", Item.ANY));
        Date dateAccepted = getDate(getMetadata(itemService.getMetadata(refItem, "refterms", "dateAccepted",null, Item.ANY)));

        if(author != null && !refAuthor.toLowerCase().contains(author.toLowerCase()))	{
            return false;
        }
        else if(startDate != null && (dateAccepted == null || dateAccepted.before(startDate)))	{
            return false;
        }
        else if(stopDate != null && (dateAccepted == null || dateAccepted.after(stopDate)))	{
            return false;
        }

        return true;

    }*/

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
                .register(this, List.of(Link.of("/api/" + RestModel.REF_REPORT, RestModel.REF_REPORT)));
    }

    /**
     * Convert a String to a date.
     *
     * If string only consists of year or year-month then
     * the first of the month is appended.
     * @param dateTxt  String is expected to be yyyy, yyyy-mm, yyyy-mm-dd
     * @return Date
     */
/*
    private Date getDate(String dateTxt)	{
        Date date = null;

        if(dateTxt.length() == 4)
            dateTxt += "-01-01";
        else if(dateTxt.length() == 7)
            dateTxt += "-01";

        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(dateTxt);

        }
        catch (Exception exc)	{
            log.error("Couldn't convert to date.\n" + exc.toString());
        }

        return date;

    }
*/
    /**
     * Explode an array list of Metadatavalues into a String separated by commas
     * @param mdvs array of Metadatavalues
     * @return String
     */
    /*private String getMetadata(java.util.List<MetadataValue> mdvs)	{
        StringBuilder mdvalue = new StringBuilder();

        for(MetadataValue mdv: mdvs)	{
            mdvalue.append(mdv.getValue()).append(", ");
        }

        return mdvalue.substring(0, Math.max(0, mdvalue.length()-2));
    }*/

    private Iterator<Item> getDateItems(Context context, String query) throws SearchServiceException	{
        if (query == null) {
            log.info("Date Query is null");
            return new ArrayList<Item>().iterator();
        }

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setQuery(query);
        discoverQuery.addFilterQueries("inArchive:true");
        discoverQuery.addFilterQueries("discoverable:true");
        discoverQuery.addFilterQueries("-withdrawn:true");

        log.info("Date Query starting search: {}", discoverQuery.getQuery());
        DiscoverResult result = searchService.search(context, discoverQuery);
        List<IndexableObject> objects = result.getIndexableObjects();
        Iterator<Item> items = objects.stream()
                .filter(obj -> obj instanceof Item)
                .map(obj -> (Item) obj)
                .collect(Collectors.toList()).iterator();

        return items;
    }

    private Iterator<Item> getAuthorItems(Context context, String author) throws SearchServiceException, SQLException {
        if (author == null) {
            log.info("Author Query is null");
            return new ArrayList<Item>().iterator();
        }

        MetadataSchema schemaDC = metadataSchemaService.find(context, "dc");
        MetadataField fieldAuthor = metadataFieldService.findByElement(context, schemaDC, "contributor", "author");
        QueryPredicate predicate = QueryPredicate.of(fieldAuthor, QueryOperator.MATCHES, ".*" + author + ".*");
        List<Collection> collections = collectionService.findAll(context);
        List<UUID> uuids = collections.stream()
                .map(Collection::getID)
                .toList();
        Iterator<Item> items = itemService.findByMetadataQuery(context, List.of(predicate), uuids, 0, -1).iterator();
        return items;
    }
}
