package com.yahoo.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.mail.search.SearchException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.ImapRequest;
import com.yahoo.imapnio.async.request.SubscribeFolderCommand;


/**
 * Unit test for {@code SubscribeFolderCommand}.
 */
public class SubscribeFolderCommandTest {
    /** Literal for SUBSCRIBE. */
    private static final String SUBSCRIBE = "SUBSCRIBE ";

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = SubscribeFolderCommand.class;
        fieldsToCheck = new HashSet<>();
        for (Class<?> c = classUnderTest; c != null; c = c.getSuperclass()) {
            for (final Field declaredField : c.getDeclaredFields()) {
                if (!declaredField.getType().isPrimitive() && !Modifier.isStatic(declaredField.getModifiers())) {
                    declaredField.setAccessible(true);
                    fieldsToCheck.add(declaredField);
                }
            }
        }
    }

    /**
     * Tests getCommandLine method.
     * 
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLine() throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final String folderName = "folderABC";
        final ImapRequest cmd = new SubscribeFolderCommand(folderName);
        Assert.assertEquals(cmd.getCommandLine(), SUBSCRIBE + folderName + "\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with folder name containing space.
     * 
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     * @throws IOException will not throw
     */
    @Test
    public void testGetCommandLineWithEscapeChar() throws IOException, SearchException, ImapAsyncClientException {
        final String folderName = "folder ABC";
        final ImapRequest cmd = new SubscribeFolderCommand(folderName);
        Assert.assertEquals(cmd.getCommandLine(), SUBSCRIBE + "\"" + folderName + "\"\r\n", "Expected result mismatched.");
    }

    /**
     * Tests getCommandLine method with folder name with other character set encoding.
     * 
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     * @throws IOException will not throw
     */
    @Test
    public void testGetCommandLineWithOtherCharSet() throws IOException, SearchException, ImapAsyncClientException {
        final String folderName = "测试";
        final ImapRequest cmd = new SubscribeFolderCommand(folderName);
        Assert.assertEquals(cmd.getCommandLine(), SUBSCRIBE + "&bUuL1Q-\r\n", "Expected result mismatched.");
    }
}