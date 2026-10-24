package br.com.muxe.core.bukkit.effect;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class Anchors {

    private Anchors() {
    }

    public static Location inFrontOfEyes(Location eyes, double distance, double contentHeight) {
        Vector forward = eyes.getDirection().setY(0);
        if (forward.lengthSquared() == 0) {
            forward = new Vector(0, 0, 1);
        }
        forward.normalize().multiply(distance);
        Location anchor = eyes.clone().add(forward);
        anchor.setY(eyes.getY() - contentHeight / 2);
        anchor.setPitch(0);
        return anchor;
    }

    public static Location lookingAt(Location contentCenter, Location viewerEyes, double contentHeight) {
        Vector towardsContent = contentCenter.toVector().subtract(viewerEyes.toVector()).setY(0);
        Location anchor = contentCenter.clone();
        if (towardsContent.lengthSquared() > 0) {
            anchor.setDirection(towardsContent);
        }
        anchor.setPitch(0);
        anchor.setY(contentCenter.getY() - contentHeight / 2);
        return anchor;
    }
}
