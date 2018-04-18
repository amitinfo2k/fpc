/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty;

import java.util.concurrent.atomic.AtomicLong;


/**
 * Convenience Class for Notifications.
 */
public class NotificationInfo {
    private static AtomicLong notifcationId = new AtomicLong();
    static public Long next() {
        return notifcationId.getAndIncrement();
    }
}
