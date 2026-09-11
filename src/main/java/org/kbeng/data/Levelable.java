package org.kbeng.data;

/**
 * Defines the levelable contract.
 */
public interface Levelable {
    /**
     * Returns the level.
     * @return {@code int}; the level
     */
    int getLevel();
    /**
     * Sets the level.
     * @param level the {@code int} supplied as {@code level}
     */
    void setLevel(int level);
    /**
     * Returns the experience.
     * @return {@code int}; the experience
     */
    int getExperience();
    /**
     * Sets the experience.
     * @param experience the {@code int} supplied as {@code experience}
     */
    void setExperience(int experience);
    /**
     * Returns the experience for next level.
     * @return {@code int}; the experience for next level
     */
    int getExperienceForNextLevel();
    /**
     * Sets the experience for next level.
     * @param experienceForNextLevel the {@code int} supplied as {@code experienceForNextLevel}
     */
    void setExperienceForNextLevel(int experienceForNextLevel);
    /**
     * Calculates next level from the current inputs.
     * @return {@code int}; the calc next level result
     */
    int calcNextLevel();
    /**
     * Advances this object to the next progression level and updates dependent statistics.
     */
    void levelUp();
    /**
     * Adds the supplied amount to accumulated progression and applies any resulting transitions.
     * @param experience the {@code int} supplied as {@code experience}
     */
    void gain(int experience);
}
