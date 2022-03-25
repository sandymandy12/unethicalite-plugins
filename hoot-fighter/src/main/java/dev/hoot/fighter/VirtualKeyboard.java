package dev.hoot.fighter;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class VirtualKeyboard extends Robot
{
    public VirtualKeyboard() throws AWTException
    {
        super();
    }

    public void pressKeys(String keysCombination) throws IllegalArgumentException
    {
        for (String key : keysCombination.split("\\+"))
        {
            try
            {
                this.keyPress((int) KeyEvent.class.getField(key.toUpperCase()).getInt(null));
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();

            }
            catch(NoSuchFieldException e )
            {
                throw new IllegalArgumentException(key.toUpperCase()+" is invalid key\n"+"VK_"+key.toUpperCase() + " is not defined in java.awt.event.KeyEvent");
            }
        }
    }

    public void releaseKeys(String keysCombination) throws IllegalArgumentException
    {
        for (String key : keysCombination.split("\\+"))
        {
            try
            { // KeyRelease method inherited from java.awt.Robot
                this.keyRelease((int) KeyEvent.class.getField(key.toUpperCase()).getInt(null));

            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch(NoSuchFieldException e )
            {
                throw new IllegalArgumentException(key.toUpperCase()+" is invalid key\n"+key.toUpperCase() + " is not defined in java.awt.event.KeyEvent");
            }
        }
    }

    public static void sendKeys(int keyCombination)
    {
        VirtualKeyboard kb = null;
        try {
            kb = new VirtualKeyboard();
            kb.keyPress(keyCombination);
            kb.keyRelease(keyCombination);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

}
