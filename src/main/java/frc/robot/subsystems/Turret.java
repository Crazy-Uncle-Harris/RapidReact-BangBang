/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMax.SoftLimitDirection;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import frc.robot.Constants;

public class Turret extends SubsystemBase {

  private final CANSparkMax m_turretSparkMax = new CANSparkMax(Constants.TurrentConstants.TurretSparkMax, MotorType.kBrushless);
  private SparkMaxPIDController pidController;
  private RelativeEncoder m_encoder;

  // value that store the current position of turret
  private double current = 0;
  private boolean aquireTarget = false;

  // neo 550 ppr = 42
  
  // gearbox pulley 36 tooth
  // turret pully 210 tooth

  // gearbox to turret ratio : 5.833333 (210 / 36)
  // gear box ratio : 100:1 (per mark)
  // motor to turret ratio  583.33:1 (5.8333 * 100)
  // click to to turret revolutions : 24499.86 (583.33 * 42)
  // clicks per degree of rotation : 68.0551667; (24499.86 / 360)
  // degress of freedom : 60 (30 left, 30 right)
  // max click offset allowed per direction : 2041.655 (30 * 68.0551667)

  private double clicksPerDegree = -1.75;//-1.6666667;
  private double m_xOffset = 0.0; // x offset reported by limelight
  private final int maxOffset = 60; // Maximum x offset allow
  private final int tolerance = 2; // clicks off target

  public Turret() {

    m_turretSparkMax.restoreFactoryDefaults();
    m_turretSparkMax.clearFaults();
    m_turretSparkMax.setSmartCurrentLimit(40, 20, 10);
    m_turretSparkMax.enableVoltageCompensation(12);
    m_turretSparkMax.setIdleMode(IdleMode.kBrake);
    m_turretSparkMax.setClosedLoopRampRate(1);

    m_encoder = m_turretSparkMax.getEncoder();
    pidController = m_turretSparkMax.getPIDController();

    pidController.setFeedbackDevice(m_encoder);

    pidController.setP(0.025 ,0);
    pidController.setI(0.0, 0);
    pidController.setD(0.04, 0);
    pidController.setIZone(0.0, 0);
    pidController.setFF(0.0001, 0);
    pidController.setOutputRange(-1, 1, 0);

    m_turretSparkMax.enableSoftLimit(SoftLimitDirection.kForward, true);
    m_turretSparkMax.enableSoftLimit(SoftLimitDirection.kReverse, true);

    m_turretSparkMax.setSoftLimit(SoftLimitDirection.kForward, maxOffset);
    m_turretSparkMax.setSoftLimit(SoftLimitDirection.kReverse, -maxOffset);

    //pre-flight checklist to make sure turrret is face directly backwards
    m_encoder.setPosition(0.0);
    pidController.setSmartMotionAllowedClosedLoopError(tolerance,0);

    SmartDashboard.putBoolean("onTarget", false);
    SmartDashboard.putBoolean("Aquire Target", false);
  }

  @Override
  public void periodic() {

    SmartDashboard.putBoolean("onTarget", this.onTarget());
    SmartDashboard.putBoolean("Aquire Target", this.aquireTarget);
    SmartDashboard.putNumber("turret error", this.m_xOffset);
    SmartDashboard.putNumber("Current turret position", this.current);

    if(aquireTarget){//Bombardier notified turrent to target
      // TrackTarget returns the offset to the target in degrees
      // if limelight has a valid target, if no valid target is found
      // TrackTarget returns no offset(0.0)
      // 1.)add the offset to current position
      current = current + trackTarget();
      SmartDashboard.putNumber("Current turret position", current);
      //Only apply changes that are less than 90 degrees off starting position
      //if target positions is greater than 90 return 90 with the proper sign(+/-)
      //current = Math.abs(current) <= maxOffset ? current : (Math.signum(current) * maxOffset);
      // 2.) update pid setpoint to new position

      pidController.setReference(current, ControlType.kPosition);
      
      // 4.) update current positoin to position after adjustment and delay
      current = m_encoder.getPosition();
    }
    SmartDashboard.putNumber("Current turret position", current);
  }
  // OI function *******************************************************************
  public void targetingEnabled(double in_XOffset){
    //Turret will auto-aim towards target
    this.aquireTarget = true;
    //get x offset from limelight
    m_xOffset = in_XOffset;
    SmartDashboard.putNumber("m_xOffset", m_xOffset);
  }

  public void targetingDisabled(boolean Reset){
    //Turret will stop auto-aiming towards target
    // and return to center back position
    this.aquireTarget = false;
    

    if(Reset){
      this.reset();
    }
  }

  public void reset(){
          //reset x offset
          m_xOffset = 0.0;

          //recenter turret on back of robot
          current = 0.0;
          pidController.setReference(current, ControlType.kPosition);
  }

  public void targetingDisabledNoParam(){
    this.targetingDisabled(false);
  }
  // end OI functions *******************************************************************


  // vision functions *******************************************************************
  private double trackTarget()
  {
    SmartDashboard.putNumber("m_xOffset * dptp", m_xOffset * clicksPerDegree);
    // TrackTarget returns the offset to the target in turret pulses (+/-)  
    return m_xOffset * clicksPerDegree;
    
  }

  public boolean onTarget(){
    // is the pid reporting that on the setpoint within the tolerance
    boolean  retVal = false;
    if(this.aquireTarget){
      retVal = Math.abs(m_xOffset) < tolerance;
    }
    return retVal;
  }

  //***********positive***********************************
  public void turn1degreesPositive(){
    this.turnToDegree(1.0);
  }

  public void turn2degreesPositive(){
    this.turnToDegree(2.0);
  }

  public void turn5degreesPositive(){
    this.turnToDegree(5.0);
  }

  public void turn10degreesPositive(){
    this.turnToDegree(10.0);
  }
  public void turn20degreesPositive(){
    this.turnToDegree(20.0);
  }

  public void turn30degreesPositive(){
    this.turnToDegree(30.0);
  }

  public void turn45degreesPositive(){
    this.turnToDegree(45.0);
  }
  //**************negative********************************
  public void turn1degreesNegative(){
    this.turnToDegree(-1.0);
  }

  public void turn2degreesNegative(){
    this.turnToDegree(-2.0);
  }

  public void turn5degreesNegative(){
    this.turnToDegree(-5.0);
  }

  public void turn10degreesNegative(){
    this.turnToDegree(-10.0);
  }

  public void turn20degreesNegative(){
    this.turnToDegree(-20.0);
  }

  public void turn30degreesNegative(){
    this.turnToDegree(-30.0);
  }

  public void turn45degreesNegative(){
    this.turnToDegree(-45.0);
  }
  //*******************************************************

  private void turnToDegree(double degrees){
    System.out.println("turnToDegree:" + degrees);
    this.current = this.current + (degrees * this.clicksPerDegree);
    System.out.println("Current:" + this.current);
    System.out.println("degress to clicks:" + (degrees * this.clicksPerDegree));

    SmartDashboard.putNumber("Current", this.current);
    pidController.setReference(this.current, ControlType.kPosition);

  }

  // end vision functions *******************************************************************
}
