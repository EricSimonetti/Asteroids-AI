package ships.y2020.simonetti;

import asteroidsfw.Vector2d;
import asteroidsfw.ai.*;

import java.util.ArrayList;

public class YellowBelly implements ShipMind{
    ShipControl control;
    int state;
    double timer;
    double collisionSensitivity;
    double deltaT;
    @Override
    public void init(ShipControl control) {
        this.control = control;
        state = 0;
        timer = 0;
        collisionSensitivity = 5;
        deltaT = 0;
    }

    @Override
    public void think(Perceptions percepts, double delta) {
        if(state == 0){
            start();
        }
        if(state == 1 || state == 2){
            evasiveManeuvers(percepts);
        }
        if(state == 3){
            if(deltaT>0){
                deltaT-=delta;
            }
            else if(percepts.asteroids().length>0) {
                offensiveManeuvers(percepts, percepts.asteroids()[0]);
                Vector2d relVel = new Vector2d(thisShip(percepts).v().x() - percepts.asteroids()[0].v().x(),
                                               thisShip(percepts).v().y() - percepts.asteroids()[0].v().y());
                if(relVel.length()>75+percepts.asteroids()[0].v().length()){ //if relative velocity is too high evasion fails
                    state = 1;                                               //basically: head on collision trajectories are a no-go,
                }                                                            //so we tap out if that's ever the case
            }
        }

        ArrayList<AsteroidPerception> collisions = calculateCollisions(percepts);
        if (!collisions.isEmpty()) {
            state = 1;
        }
        else if(state != 0){
            state = 3;
        }

        if(checkForShot(percepts)&&thisShip(percepts).shotCoolDown()<0){
            control.shooting(true);
        }
        else{
            control.shooting(false);
        }


        if(percepts.asteroids().length==1 && percepts.asteroids()[0].radius()==5){
            collisionSensitivity = 2; //makes ship more aggressive because there's only one asteroid left
            if(percepts.asteroids()[0].pos().x()<400 && percepts.asteroids()[0].pos().y()<400){
                collisionSensitivity = 7; //makes ship less aggressive because that's where the new asteroids will spawn
            }
        }
        else{
            collisionSensitivity = 3;
        }

        if(thisShip(percepts).pos().x()==0&&thisShip(percepts).pos().y()==0){
            state = 3;
        }
    }

    private void start(){
        if(control.pos().x()<411) {
            control.thrustForward(true);
            control.thrustBackward(false);
            control.rotateLeft(true);
        }
        if(control.pos().x()>411) {
            control.rotateLeft(false);
            control.thrustForward(false);
            state = 3;
        }
    }
    private ArrayList<AsteroidPerception> calculateCollisions(Perceptions percepts){
        ArrayList<AsteroidPerception> collisions = new ArrayList<>();

        double tWrap = aboutToWrap(thisShip(percepts).pos(), thisShip(percepts).v());
        ArrayList<Vector2d> positions = new ArrayList<>();
        positions.add(thisShip(percepts).pos());
        if(tWrap!=-1){
            Vector2d wrap = new Vector2d(positions.get(0).x()+thisShip(percepts).v().x()*tWrap,
                                         positions.get(0).y()-thisShip(percepts).v().y()*tWrap);
            positions.add(wrap);
        }
        for(int p = 0; p<positions.size(); p++) {
            for (int i = 0; i < percepts.asteroids().length; i++) {
                double r = percepts.asteroids()[i].radius() + 10;
                Vector2d relPos = new Vector2d(positions.get(p).x() - percepts.asteroids()[i].pos().x(),
                                               positions.get(p).y() - percepts.asteroids()[i].pos().y());
                Vector2d relVel = new Vector2d(thisShip(percepts).v().x() - percepts.asteroids()[i].v().x(),
                                               thisShip(percepts).v().y() - percepts.asteroids()[i].v().y());

                double tClosest = -relPos.dot(relVel) / relVel.dot(relVel);
                Vector2d closest = new Vector2d(relPos.x() + relVel.x() * tClosest,
                                                relPos.y() + relVel.y() * tClosest);

                //System.out.println(p + "  " + tClosest + "    " + closest.length());
                double cSensitivity = p==0?collisionSensitivity:collisionSensitivity-tWrap;
                if (tClosest < cSensitivity && tClosest > 0 && closest.length() <= r) {
                    collisions.add(percepts.asteroids()[i]);
                    deltaT = tClosest;
                }
                //if(tClosest<.2 && tClosest>0){
                //    control.thrustForward(false);
                //    control.thrustBackward(true);
                //}
            }
        }
        return collisions;
    }

    private double aboutToWrap(Vector2d pos, Vector2d v){
        double tLeft = (0 - pos.x())/v.x();
        double tRight = (800 - pos.x())/v.x();
        double tUp = (0-pos.y())/v.y();
        double tDown = (600-pos.y())/v.y();
        tLeft = tLeft>0?tLeft:1000;
        tRight = tRight>0?tRight:1000;
        tUp = tUp>0?tUp:1000;
        tDown = tDown>0?tDown:1000;

        //System.out.println(tUp + "  " + pos.y() + "  " + tRight + "    " + pos.x());

        double tWrap = Math.min(Math.min(tLeft, tRight), Math.min(tUp, tDown));

        return tWrap<5?tWrap:-1;

    }

    private void offensiveManeuvers(Perceptions percepts, AsteroidPerception asteroid){
        /*
        control.rotateLeft(true);
        control.rotateRight(false);
        control.thrustForward(false);
        control.thrustBackward(false);

         */
        ///*
        double vAng = Math.acos(thisShip(percepts).v().x()/thisShip(percepts).v().length());
        Vector2d v = new Vector2d(Math.cos(vAng), Math.sin(vAng));

        Vector2d direction = thisShip(percepts).direction();
        Vector2d relPos = new Vector2d( asteroid.pos().x() - thisShip(percepts).pos().x(),
                                        asteroid.pos().y() - thisShip(percepts).pos().y());
        double desiredDirection = Math.acos(relPos.x()/relPos.length());
        double currentDirection = Math.acos(direction.x()/direction.length());
        if(asteroid.pos().y()>thisShip(percepts).pos().y()){
            //desiredDirection = 2*Math.PI - desiredDirection;
        }
        double diff = desiredDirection - currentDirection;

        //System.out.println(diff);
        if(Math.abs(diff)<.2){
            /*
            if(v.length()>75 && v.dot(direction)>.5){
                control.thrustBackward(true);
                control.thrustForward(false);
            }
            else{
             */
            control.thrustForward(true);
            control.thrustBackward(false);
            //}
            control.rotateRight(false);
            control.rotateLeft(false);
        }
        else{
            control.rotateRight(true);
            control.rotateLeft(false);
        }
        /*
        else if(diff>0&&diff>Math.PI){
            control.rotateLeft(false);
            control.rotateRight(true);
        }
        else if(diff>0&&diff<Math.PI){
            control.rotateLeft(false);
            control.rotateRight(true);
        }
        else if(diff<0&&diff>-Math.PI){
            control.rotateLeft(false);
            control.rotateRight(true);
        }
        else{
            control.rotateLeft(true);
            control.rotateRight(false);
        }
         //*/
    }

    private void evasiveManeuvers(Perceptions percepts){
        //System.out.println("EVASIVE MANEUVERS");
        if(state == 1){
            turn90AndThrust(percepts);
        }
        else{
            control.thrustForward(true);
        }
    }

    private void turn90AndThrust(Perceptions percepts){
        double goalAng = Math.PI/2;

        Vector2d curr = thisShip(percepts).direction();
        Vector2d vel = thisShip(percepts).v();
        double velAng = Math.acos(vel.x()/vel.length());
        double currAng = Math.acos(curr.x()/curr.length());
        double diff = velAng-currAng;
        System.out.println(diff + " " + velAng + " " + currAng + " " + goalAng);
        if(Math.abs(diff)<goalAng+.1 && Math.abs(diff)>goalAng-.1){
            state = 2;
            control.thrustForward(true);
            control.thrustBackward(false);
            control.rotateRight(false);
            control.rotateLeft(false);
        }
        else if(diff>(goalAng)){
            control.rotateLeft(true);
            control.rotateRight(false);
        }
        else if(diff<(goalAng)&&diff>0){
            control.rotateLeft(true);
            control.rotateRight(false);
        }
        else if(diff > -goalAng){
            control.rotateLeft(true);
            control.rotateRight(false);
        }
        else{
            control.rotateLeft(false);
            control.rotateRight(true);
        }


        if(Double.isNaN(velAng)){
            control.thrustBackward(true);
        }
        else{
            control.thrustBackward(false);
        }
    }

    private boolean checkForShot(Perceptions percepts){
        boolean hasShot = false;
        for (int i = 0; i < percepts.asteroids().length; i++) {
            double r = percepts.asteroids()[i].radius()-4;
            Vector2d v = new Vector2d(thisShip(percepts).v().x() + thisShip(percepts).direction().x()*150,
                                      thisShip(percepts).v().y() + thisShip(percepts).direction().y()*150);
            Vector2d relPos = new Vector2d(thisShip(percepts).pos().x() - percepts.asteroids()[i].pos().x(),
                                           thisShip(percepts).pos().y() - percepts.asteroids()[i].pos().y());
            Vector2d relVel = new Vector2d(v.x() - percepts.asteroids()[i].v().x(),
                                           v.y() - percepts.asteroids()[i].v().y());

            double tClosest = -relPos.dot(relVel) / relVel.dot(relVel);
            Vector2d closest = new Vector2d(relPos.x() + relVel.x() * tClosest,
                    relPos.y() + relVel.y() * tClosest);
            hasShot = hasShot || (tClosest<8 && tClosest>0 && closest.length()<r);

            if(hasShot){
                //System.out.println("  " + tClosest + "    " + closest.length());
            }
        }
        return hasShot;
    }


    private ShipPerception thisShip(Perceptions percepts){
        for(int i = 0; i<percepts.ships().length; i++){
            if(percepts.ships()[i].equals(control)){
                return percepts.ships()[i];
            }
        }
        return control;
    }
}