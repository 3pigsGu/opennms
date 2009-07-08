/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Copyright (C) 2002-2004, 2006, 2008 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc.:
 *
 *      51 Franklin Street
 *      5th Floor
 *      Boston, MA 02110-1301
 *      USA
 *
 * For more information contact:
 *
 *      OpenNMS Licensing <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 *
 *******************************************************************************/


package org.opennms.core.queue;

/**
 * <p>
 * This interface is implemented by objects that need to be notified when a new
 * element is added to a queue. The notification method is invoked aftet the
 * element is added to the queue, the exact semantics of which are defined by
 * the queue.
 * </p>
 * 
 * <p>
 * This is most often used to notify a class that an empty queue has new
 * elements that need to be processed. This allows the object to perform other
 * potentially useful work while waiting on new queue elements.
 * </p>
 * 
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 * @author <a href="http://www.opennms.org">OpenNMS </a>
 * 
 */
public interface InputFifoQueueListener {
    /**
     * This method is invoked by a queue implementation when a new element is
     * added its queue. The exact instance when the method is invoked is
     * dependent upon the implementation.
     * 
     * @param queue
     *            The queue where the element was added.
     */
    public void onQueueInput(NotifiableInputFifoQueue queue);
}
