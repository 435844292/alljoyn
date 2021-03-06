/*
 * Copyright (c) 2011-2012, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
#include "SuccessListenerNative.h"

#include <qcc/Debug.h>

#define QCC_MODULE "ALLJOYN_JS"

SuccessListenerNative::SuccessListenerNative(Plugin& plugin, NPObject* objectValue) :
    NativeObject(plugin, objectValue)
{
    QCC_DbgTrace(("%s", __FUNCTION__));
}

SuccessListenerNative::~SuccessListenerNative()
{
    QCC_DbgTrace(("%s", __FUNCTION__));
}

void SuccessListenerNative::onSuccess()
{
    QCC_DbgTrace(("%s", __FUNCTION__));
    NPVariant result = NPVARIANT_VOID;
    NPN_InvokeDefault(plugin->npp, objectValue, 0, 0, &result);
    NPN_ReleaseVariantValue(&result);
}

