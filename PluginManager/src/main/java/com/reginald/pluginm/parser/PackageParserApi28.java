/*
**        DroidPlugin Project
**
** Copyright(c) 2015 Andy Zhang <zhangyong232@gmail.com>
**
** This file is part of DroidPlugin.
**
** DroidPlugin is free software: you can redistribute it and/or
** modify it under the terms of the GNU Lesser General Public
** License as published by the Free Software Foundation, either
** version 3 of the License, or (at your option) any later version.
**
** DroidPlugin is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
** Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public
** License along with DroidPlugin.  If not, see <http://www.gnu.org/licenses/lgpl.txt>
**
**/

package com.reginald.pluginm.parser;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;

import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.utils.Logger;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Andy Zhang(zhangyong232@gmail.com) on 2015/5/29.
 */
//for Android M
class PackageParserApi28 extends PackageParserApi22 {


    private static final String TAG = PackageParserApi28.class.getSimpleName();

    PackageParserApi28(Context context) throws Exception {
        super(context);
    }

    @Override
    public void collectCertificates(int flags) throws Exception {
        // public void collectCertificates(Package pkg, int flags) throws PackageParserException
        Method method = MethodUtils.getAccessibleMethod(sPackageParserClass, "collectCertificates",
                mPackage.getClass(), boolean.class);
        method.invoke(mPackageParser, mPackage, true);
    }

    @Override
    public void writeSignature(Signature[] signatures) throws Exception {
        // TODO write signatures on Android P
    }
}
