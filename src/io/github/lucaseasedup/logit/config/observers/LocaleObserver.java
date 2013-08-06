/*
 * LocaleObserver.java
 *
 * Copyright (C) 2012-2013 LucasEasedUp
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.lucaseasedup.logit.config.observers;

import io.github.lucaseasedup.logit.LogItCore;
import io.github.lucaseasedup.logit.config.Property;
import io.github.lucaseasedup.logit.config.PropertyObserver;
import java.io.IOException;
import java.util.logging.Level;

/**
 * @author LucasEasedUp
 */
public class LocaleObserver extends PropertyObserver
{
    public LocaleObserver(LogItCore core)
    {
        super(core);
    }
    
    @Override
    public void update(Property p)
    {
        try
        {
            core.getPlugin().loadMessages();
        }
        catch (IOException ex)
        {
            core.log(Level.WARNING, "Could not load messages. Stack trace:");
            ex.printStackTrace();
        }
    }
}