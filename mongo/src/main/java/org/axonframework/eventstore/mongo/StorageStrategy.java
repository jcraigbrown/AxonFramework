/*
 * Copyright (c) 2010-2012. Axon Framework
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

package org.axonframework.eventstore.mongo;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.eventstore.mongo.criteria.MongoCriteria;
import org.axonframework.serializer.Serializer;
import org.axonframework.upcasting.UpcasterChain;

import java.util.List;

/**
 * Interface towards the mechanism that prescribes the structure in which events are stored in the Event Store. Events
 * are provided in "commits", which represent a number of events generated by the same aggregate, inside a single
 * Unit of Work. Implementations may choose to use this fact, or ignore it.
 *
 * @author Allard Buijze
 * @since 2.0
 */
public interface StorageStrategy {

    /**
     * Generates the DBObject instances that need to be stored for a commit.
     *
     * @param type            The aggregate's type identifier
     * @param eventSerializer The serializer to serialize events with
     * @param messages        The messages contained in this commit
     * @return an array of DBObject, representing the documents to store
     */
    DBObject[] createDocuments(String type, Serializer eventSerializer, List<DomainEventMessage> messages);

    /**
     * Extracts the individual Event Messages from the given <code>entry</code>. The <code>aggregateIdentifier</code>
     * is passed to allow messages to contain the actual object, instead of its serialized form. The
     * <code>serializer</code> and <code>upcasterChain</code> should be used to deserialize and upcast messages before
     * returning them.
     *
     * @param entry               The entry containing information of a stored commit
     * @param aggregateIdentifier The aggregate identifier used to query events
     * @param serializer          The serializer to deserialize events with
     * @param upcasterChain       The upcaster chain to upcast stored events with
     * @param skipUnknownTypes    Whether unknown event types should be skipped
     * @return a list of DomainEventMessage contained in the entry
     */
    List<DomainEventMessage> extractEventMessages(DBObject entry, Object aggregateIdentifier, Serializer serializer,
                                                  UpcasterChain upcasterChain, boolean skipUnknownTypes);

    /**
     * Provides a cursor for access to all events for an aggregate with given <code>aggregateType</code> and
     * <code>aggregateIdentifier</code>, with a sequence number equal or higher than the given
     * <code>firstSequenceNumber</code>. The returned documents should be ordered chronologically (typically by using
     * the sequence number).
     * <p/>
     * Each DBObject document returned as result of this cursor will be passed to {@link
     * #extractEventMessages} in order to retrieve individual DomainEventMessages.
     *
     * @param collection          The collection to
     * @param aggregateType       The type identifier of the aggregate to query
     * @param aggregateIdentifier The identifier of the aggregate to query
     * @param firstSequenceNumber The sequence number of the first event to return
     * @return a Query object that represent a query for events of an aggregate
     */
    DBCursor findEvents(DBCollection collection, String aggregateType, String aggregateIdentifier,
                        long firstSequenceNumber);

    /**
     * Find all events that match the given <code>criteria</code> in the given <code>collection</code>
     *
     * @param collection The collection to search for events
     * @param criteria   The criteria to match against the events
     * @return a cursor for the documents representing matched events
     */
    DBCursor findEvents(DBCollection collection, MongoCriteria criteria);

    /**
     * Finds the entry containing the last snapshot event for an aggregate with given <code>aggregateType</code> and
     * <code>aggregateIdentifier</code> in the given <code>collection</code>. For each result returned by the Cursor,
     * an invocation to {@link #extractEventMessages} will be used to extract
     * the actual DomainEventMessages.
     *
     * @param collection          The collection to find the last snapshot event in
     * @param aggregateType       The type identifier of the aggregate to find a snapshot for
     * @param aggregateIdentifier The identifier of the aggregate to find a snapshot for
     * @return a cursor providing access to the entries found
     */
    DBCursor findLastSnapshot(DBCollection collection, String aggregateType, String aggregateIdentifier);

    /**
     * Ensure that the correct indexes are in place.
     *
     * @param eventsCollection    The collection containing the documents representing commits and events.
     * @param snapshotsCollection The collection containing the document representing snapshots
     */
    void ensureIndexes(DBCollection eventsCollection, DBCollection snapshotsCollection);
}
