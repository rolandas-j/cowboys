package com.rolandas.rjtrader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) throws InterruptedException {
    List<Cowboy> cowboys =  new ArrayList<>();

    AtomicBoolean startFlag = new AtomicBoolean(false);
    AtomicBoolean endFlag = new AtomicBoolean(false);
    cowboys.add(new Cowboy("John", 10, 1));
    cowboys.add(new Cowboy("Bill", 8, 2));
    cowboys.add(new Cowboy("Sam", 10, 1));
    cowboys.add(new Cowboy("Peter", 5, 3));
    cowboys.add(new Cowboy("Philip", 15, 1));
    ExecutorService cowboysExecutor = Executors.newFixedThreadPool(5);
    for (int i = 0; i < cowboys.size(); i++) {
      final int cowboyIndex = i;
      final Cowboy myself = cowboys.get(cowboyIndex);
      final Random random = new Random();
      cowboysExecutor.submit(() -> {
        while(myself.isAlive() && !endFlag.get()){
          if(!startFlag.get()) {
            continue;
          }
          int shootIndex = random.nextInt(5);
          if (shootIndex == cowboyIndex) {
            continue;
          }
          Cowboy target = cowboys.get(shootIndex);
          System.out.printf("%s shoot %s on %s damage%n", myself.getName(), target.getName(), myself.getDamage());
          target.shoot(myself.getDamage());
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      });
    }

    ExecutorService barmen = Executors.newSingleThreadExecutor();
    barmen.submit(() -> {
      System.out.println("Fight!!!!");
      startFlag.set(true);
      boolean lastManStanding = false;
      while(!lastManStanding) {
        List<Cowboy> aliveCowboys = cowboys.stream().filter(Cowboy::isAlive).toList();
        if (aliveCowboys.size() == 1) {
          System.out.printf("%s wins!", aliveCowboys.get(0).getName());
          endFlag.set(true);
          lastManStanding = true;
        }
      }
    });

    barmen.awaitTermination(5l, TimeUnit.SECONDS);

    cowboysExecutor.shutdown();
    barmen.shutdown();
  }




}

class Cowboy {

  private String name;
  private int health;
  private int damage;
  private volatile boolean alive;

  public Cowboy(String name, int health, int damage) {
    this.name = name;
    this.health = health;
    this.damage = damage;
    this.alive = true;
  }

  public synchronized boolean shoot(int damage) {
    this.health = this.health - damage;
    if (this.health <= 0) {
      this.alive = false;
      System.out.println(String.format("Cowboy %s died", this.name));
    }
    return this.alive;
  }

  public String getName() {
    return name;
  }

  public int getHealth() {
    return health;
  }

  public int getDamage() {
    return damage;
  }

  public void setDamage(int damage) {
    this.damage = damage;
  }

  public boolean isAlive() {
    return alive;
  }
}