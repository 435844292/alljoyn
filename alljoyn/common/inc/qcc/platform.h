#ifndef _QCC_PLATFORM_H
#define _QCC_PLATFORM_H
/**
 * @file
 *
 * This file just wraps including actual OS and toolchain specific header
 * files depding on the OS group setting.
 */

/******************************************************************************
 * Copyright (c) 2010-2011, 2014 AllSeen Alliance. All rights reserved.
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
 ******************************************************************************/

#if defined(QCC_OS_GROUP_POSIX)
#include <qcc/posix/platform_types.h>
#elif defined(QCC_OS_GROUP_WINDOWS)
#include <qcc/windows/platform_types.h>
#include <qcc/windows/mapping.h>
#else
#error No OS GROUP defined.
#endif

#if defined(__GNUC__)

#define GCC_VERSION ((__GNUC__ * 10000) + (__GNUC_MINOR__ * 100) + __GNUC_PATCHLEVEL__)
#if (GCC_VERSION < 40700L)
/*
 * Versions of GCC prior to 4.7.0 have an annoying but intentional bug where
 * __cplusplus is set to 1 rather than the appropriate date code so that it
 * would be compatible with Solaris 8.
 */

#if (GCC_VERSION >= 40600L) && defined(__GXX_EXPERIMENTAL_CXX0X__)
/*
 * GCC 4.6.x supports C++11, at least in terms of unordered_map, etc. when the
 * -std=gnu++0x option is passed in.  Thus, fix the value of __cplusplus.
 */
#undef __cplusplus
#define __cplusplus 201100L
#endif  // GCC version >= 4.6 and -std=gnu++0x
#endif  // GCC version < 4.7



#if (__GNUC__ >= 4) || ((__GNUC__ == 3) && (__GNUC_MINOR__ >= 1))
#define QCC_DEPRECATED(func) func __attribute__((deprecated)) /**< mark a function as deprecated in gcc. */
#else
#define QCC_DEPRECATED(func) func /**< not all gcc versions support the deprecated attribute. */
#endif


#elif defined(_MSC_VER)

#define QCC_DEPRECATED(func) __declspec(deprecated) func /**< mark a function as deprecated in msvc. */


#else /* Some unknown compiler */

#define QCC_DEPRECATED(func); /**< mark a function as deprecated. */

#endif /* Compiler type */

/**
 * Macro used to avoid unused variable warning in release code.
 * This Macro is only used when a variable is only used in the debug build
 * variant. When building in release mode the compiler will give an unused
 * variable warning.
 *
 * Example usage:
   @code
     String errMsg
     const char* errName = reply->GetErrorName(&errMsg);
     QCC_LogError(status, ("TimedPing returned ERROR_MESSAGE (error=%s, \"%s\")", errName, errMsg.c_str()));
     QCC_UNUSED(errName); // avoid unused variable warning in release build
   @endcode
 *
 * The `QCC_LogError` is an empty macro for release build variants but in debug
 * build variants it is not empty so it uses the errName variable.
 */
#define QCC_UNUSED(x) (void)(x)

/** Boolean type for C */
typedef int32_t QCC_BOOL;
/** Boolean logic true for QCC_BOOL type*/
#define QCC_TRUE 1
/** Boolean logic false for QCC_BOOL type*/
#define QCC_FALSE 0

#ifdef __cplusplus
/** Nifty counter used to ensure that AllJoyn Globals are initialized before any other client code static
 *  or global variables
 */
static struct StaticGlobalsInit {
    StaticGlobalsInit();
    ~StaticGlobalsInit();
    static void Cleanup();

  private:
    static bool cleanedup;

} staticGlobalsInit;

#endif
#endif // _QCC_PLATFORM_H
