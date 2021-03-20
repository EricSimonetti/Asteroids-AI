package ships.y2020.example;

import asteroidsfw.Ship;
import asteroidsfw.ai.*;

public class ExampleShip implements ShipMind {
    ShipControl control;
    @Override
    public void init(ShipControl control) {
        this.control = control;
    }

    @Override
    public void think(Perceptions percepts, double delta) {
        control.thrustForward(true);
	    control.rotateRight(true);
	    control.shooting(true);
    }
}
