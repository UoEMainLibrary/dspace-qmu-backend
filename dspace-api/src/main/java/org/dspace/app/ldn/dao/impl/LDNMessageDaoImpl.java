/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.LDNMessageEntity_;
import org.dspace.app.ldn.dao.LDNMessageDao;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for
 * the LDNMessage object. This class is responsible for all database calls for
 * the LDNMessage object and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNMessageDaoImpl extends AbstractHibernateDAO<LDNMessageEntity> implements LDNMessageDao {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LDNMessageDaoImpl.class);

    @Override
    public List<LDNMessageEntity> findOldestMessageToProcess(Context context, int max_attempts) throws SQLException {
        // looking for oldest failed-processed message
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, LDNMessageEntity.class);
        Root<LDNMessageEntity> root = criteriaQuery.from(LDNMessageEntity.class);
        criteriaQuery.select(root);
        List<Predicate> andPredicates = new ArrayList<>(3);
        andPredicates
            .add(criteriaBuilder.equal(root.get(LDNMessageEntity_.queueStatus), LDNMessageEntity.QUEUE_STATUS_QUEUED));
        andPredicates.add(criteriaBuilder.lessThan(root.get(LDNMessageEntity_.queueAttempts), max_attempts));
        andPredicates.add(criteriaBuilder.lessThan(root.get(LDNMessageEntity_.queueTimeout), new Date()));
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[] {})));
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(root.get(LDNMessageEntity_.queueAttempts)));
        orderList.add(criteriaBuilder.asc(root.get(LDNMessageEntity_.queueLastStartTime)));
        criteriaQuery.orderBy(orderList);
        // setHint("org.hibernate.cacheable", Boolean.FALSE);
        List<LDNMessageEntity> result = list(context, criteriaQuery, false, LDNMessageEntity.class, -1, -1);
        if (result == null || result.isEmpty()) {
            log.debug("No LDN messages found to be processed");
        }
        return result;
    }

    @Override
    public List<LDNMessageEntity> findProcessingTimedoutMessages(Context context, int max_attempts)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, LDNMessageEntity.class);
        Root<LDNMessageEntity> root = criteriaQuery.from(LDNMessageEntity.class);
        criteriaQuery.select(root);
        List<Predicate> andPredicates = new ArrayList<>(3);
        andPredicates.add(
            criteriaBuilder.equal(root.get(LDNMessageEntity_.queueStatus), LDNMessageEntity.QUEUE_STATUS_PROCESSING));
        andPredicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(LDNMessageEntity_.queueAttempts), max_attempts));
        andPredicates.add(criteriaBuilder.lessThan(root.get(LDNMessageEntity_.queueTimeout), new Date()));
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[] {})));
        List<Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(root.get(LDNMessageEntity_.queueAttempts)));
        orderList.add(criteriaBuilder.asc(root.get(LDNMessageEntity_.queueLastStartTime)));
        criteriaQuery.orderBy(orderList);
        // setHint("org.hibernate.cacheable", Boolean.FALSE);
        List<LDNMessageEntity> result = list(context, criteriaQuery, false, LDNMessageEntity.class, -1, -1);
        if (result == null || result.isEmpty()) {
            log.debug("No LDN messages found to be processed");
        }
        return result;
    }
}
