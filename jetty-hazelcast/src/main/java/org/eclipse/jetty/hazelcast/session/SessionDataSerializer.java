//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//


package org.eclipse.jetty.hazelcast.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.util.ClassLoadingObjectInputStream;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

/**
 * SessionDataSerializer
 *
 * Handles serialization on behalf of the SessionData object, and
 * ensures that we use jetty's classloading knowledge.
 */
public class SessionDataSerializer implements StreamSerializer<SessionData>
{

    @Override
    public int getTypeId()
    {
        return 99;
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void write(ObjectDataOutput out, SessionData data) throws IOException
    {
        out.writeUTF(data.getId()); //session id
        out.writeUTF(data.getContextPath()); //context path
        out.writeUTF(data.getVhost()); //first vhost

        out.writeLong(data.getAccessed());//accessTime
        out.writeLong(data.getLastAccessed()); //lastAccessTime
        out.writeLong(data.getCreated()); //time created
        out.writeLong(data.getCookieSet());//time cookie was set
        out.writeUTF(data.getLastNode()); //name of last node managing
  
        out.writeLong(data.getExpiry()); 
        out.writeLong(data.getMaxInactiveMs());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        SessionData.serializeAttributes(data, oos);
        
        out.writeByteArray(baos.toByteArray());
        
    }

    @Override
    public SessionData read(ObjectDataInput in) throws IOException
    {        
        String id = in.readUTF();
        String contextPath = in.readUTF();
        String vhost = in.readUTF();
        
        long accessed = in.readLong();//accessTime
        long lastAccessed = in.readLong(); //lastAccessTime
        long created = in.readLong(); //time created
        long cookieSet = in.readLong();//time cookie was set
        String lastNode = in.readUTF(); //last managing node
        long expiry = in.readLong(); 
        long maxInactiveMs = in.readLong();
        
        SessionData sd = new SessionData(id, contextPath, vhost, created, accessed, lastAccessed, maxInactiveMs);

        ByteArrayInputStream bais = new ByteArrayInputStream(in.readByteArray());
        try (ClassLoadingObjectInputStream ois = new ClassLoadingObjectInputStream(bais))
        {
            SessionData.deserializeAttributes(sd, ois);
        }
        catch(ClassNotFoundException e)
        {
            throw new IOException(e);
        }
        sd.setCookieSet(cookieSet);
        sd.setLastNode(lastNode);
        sd.setExpiry(expiry);
        return sd;
    }

}
