package org.kbeng.data;

import org.kbeng.item.Item;

/**
 * Defines plantable behavior.
 /**
  * can defines the can contract within the data subsystem.
  *
  * Implementations are expected to preserve these behavioral guarantees while choosing their own storage and execution strategy.
  *
  * The implementation keeps this concern isolated so higher-level orchestrators remain focused on flow control.
  */
 * Extensions of this interface can be planted
 */
public interface Plantable extends Item {
    /**
     * Returns the crop type.
     * @return {@code CropType}
     */
    CropType getType();
}
