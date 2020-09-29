/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */
package com.sun.squawk.io.j2me.msg;

import java.util.*;

import com.sun.squawk.*;

/**
 * This class allows for the highly dubious practice of object reuse ;-).
 *
 * @author Nik Shaylor
 */

public class MessageResourceManager {

/*if[REUSEABLE_MESSAGES]*/

    /**
     * List of reuseable ClientProtocols
     */
    private static Stack freeClientProtocols = new Stack();

    /**
     * List of reuseable ServerProtocols
     */
    private static Stack freeServerProtocols = new Stack();

    /**
     * List of reuseable MessageInputStreams
     */
    private static Stack freeInputStreams = new Stack();

    /**
     * List of reuseable MessageInputStreams
     */
    private static Stack freeOutputStreams = new Stack();

    /**
     * List of reuseable MessageInputStreams
     */
    private static Stack freeMessages = new Stack();

/*end[REUSEABLE_MESSAGES]*/

    /**
     * Allocates a client protocol object
     */
    public static ClientProtocol allocateClientProtocol() {
        ClientProtocol res;
/*if[REUSEABLE_MESSAGES]*/
        if (!freeClientProtocols.isEmpty()) {
            res = (ClientProtocol)freeClientProtocols.pop();
            res.resetInstanceState();
        } else
/*end[REUSEABLE_MESSAGES]*/
        {
            res = new ClientProtocol();
        }
        return res;
    }

    /**
     * Allocates a server protocol object.
     *
     * @param name the namespace target for the message
     * @param data the input data
     */
    public static ServerProtocol allocateServerProtocol(String name, Message data) {
        ServerProtocol res;
/*if[REUSEABLE_MESSAGES]*/
        if (!freeServerProtocols.isEmpty()) {
            res = (ServerProtocol)freeServerProtocols.pop();
        } else
/*end[REUSEABLE_MESSAGES]*/
        {
            res = new ServerProtocol();
        }
        res.resetInstanceState(name, data);
        return res;
    }

    /**
     * Frees a ClientProtocol.
     *
     * @param con the connection
     */
    static void freeClientProtocol(ClientProtocol con) {
/*if[REUSEABLE_MESSAGES]*/
        freeClientProtocols.push(con);
/*end[REUSEABLE_MESSAGES]*/
    }

    /**
     * Frees a ServerProtocol.
     *
     * @param con the connection
     */
    static void freeServerProtocol(ServerProtocol con) {
/*if[REUSEABLE_MESSAGES]*/
        freeServerProtocols.push(con);
/*end[REUSEABLE_MESSAGES]*/
    }

    /**
     * Allocates a message input stream.
     *
     * @param msc the call back object
     */
    static MessageInputStream allocateInputStream(MessageStreamCallback msc) {
        MessageInputStream is;
/*if[REUSEABLE_MESSAGES]*/
        if (!freeInputStreams.isEmpty()) {
            is = (MessageInputStream)freeInputStreams.pop();
        } else
/*end[REUSEABLE_MESSAGES]*/
        {
            is = new MessageInputStream();
        }
        is.resetInstanceState(msc);
        return is;
    }

    /**
     * Frees a message input stream.
     *
     * @param is the stream
     */
    static void freeInputStream(MessageInputStream is) {
/*if[REUSEABLE_MESSAGES]*/
        freeInputStreams.push(is);
/*end[REUSEABLE_MESSAGES]*/
    }

    /**
     * Allocates a message output stream.
     *
     * @param msc the call back object
     */
    static MessageOutputStream allocateOutputStream(MessageStreamCallback msc) {
        MessageOutputStream os;
/*if[REUSEABLE_MESSAGES]*/
        if (!freeOutputStreams.isEmpty()) {
            os = (MessageOutputStream)freeOutputStreams.pop();
        } else
/*end[REUSEABLE_MESSAGES]*/
        {
            os = new MessageOutputStream();
        }
        os.resetInstanceState(msc);
        return os;
    }

    /**
     * Frees a message output stream.
     *
     * @param os the stream
     */
    static void freeOutputStream(MessageOutputStream os) {
/*if[REUSEABLE_MESSAGES]*/
        freeOutputStreams.push(os);
/*end[REUSEABLE_MESSAGES]*/
    }

    /**
     * Allocates a message.
     *
     * @return the message
     */
    static Message allocateMessage() {
        Message msg;
/*if[REUSEABLE_MESSAGES]*/
        if (!freeMessages.isEmpty()) {
            msg = (Message)freeMessages.pop();
        } else
/*end[REUSEABLE_MESSAGES]*/
        {
            msg = new Message();
        }
        msg.resetInstanceState();
        return msg;
    }

    /**
     * Allocates a message.
     *
     * @param buffers a list of buffers
     * @return the message
     */
    static Message allocateMessage(Address buffers) {
        Message msg;
/*if[REUSEABLE_MESSAGES]*/
        if (!freeMessages.isEmpty()) {
            msg = (Message)freeMessages.pop();
        } else
/*end[REUSEABLE_MESSAGES]*/
        {
            msg = new Message();
        }
        msg.setData(buffers);
        return msg;
    }

    /**
     * Frees a message.
     *
     * @param msg the message
     */
    static void freeMessage(Message msg) {
/*if[REUSEABLE_MESSAGES]*/
        msg.freeAll();
        freeMessages.push(msg);
/*end[REUSEABLE_MESSAGES]*/
    }

}


