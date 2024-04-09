package com.rolandas.rjtrader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String[] args) throws InterruptedException, IOException {

    URL url = ClassLoader.getSystemClassLoader().getResource("cowboys.json");
    if(url == null) {
      throw new IllegalStateException("File not found");
    }
    ObjectMapper mapper = new ObjectMapper();
    List<CowboyDTO> cowboysDTOs  = mapper.readValue(url, new TypeReference<>() {});
    List<Cowboy> cowboys = cowboysDTOs.stream().map(CowboyDTO::asCowboy).toList();
    Bar bar = new Bar(cowboys);

    ExecutorService cowboysExecutor = Executors.newFixedThreadPool(cowboys.size());
    cowboys.forEach(cowboy -> {
      cowboy.enterBar(bar);
      cowboysExecutor.submit(cowboy);
    });


    boolean lastManStanding = false;
    while(!lastManStanding) {
      if (bar.isOnlyOneAlive()) {
        System.out.printf("%s wins!", bar.getLastManStating().getName());
        lastManStanding = true;
      }
      Thread.sleep(1000);
    }

    cowboysExecutor.shutdown();
  }

}
class Bar {
  private final KeySetView<Cowboy, Boolean> cowboys;
  private final Random random = new Random();

  public Bar(List<Cowboy> cowboys) {
    this.cowboys = ConcurrentHashMap.newKeySet();
    this.cowboys.addAll(cowboys);
  }

  public Cowboy pickCowboy(String shooterName) {
    List<Cowboy> enemies = cowboys.stream()
        .filter(cowboy -> !cowboy.getName().equals(shooterName))
        .filter(Cowboy::isCowboyAlive)
        .toList();
    if (enemies.isEmpty()) {
      return null;
    }
    return enemies.get(random.nextInt(enemies.size()));
  }

  public void removeCorpse(Cowboy cowboy) {
    cowboys.remove(cowboy);
  }

  //In theory there is a race condition where both of the cowboys shoot each other at the same time, and we don't have a winner
  public boolean isOnlyOneAlive() {
    return this.cowboys.size() == 1;
  }

  public Cowboy getLastManStating() {
    return (Cowboy) cowboys.toArray()[0];
  }
}

record CowboyDTO(String name, int health, int damage) {
  public Cowboy asCowboy() {
    return new Cowboy(this.name, this.health, this.damage);
  }
};

class Cowboy extends Thread {

  private int health;
  private final int damage;
  private volatile boolean alive;
  private Bar bar;

  public Cowboy(String name, int health, int damage) {
    super(name);
    this.health = health;
    this.damage = damage;
    this.alive = true;
  }

  @Override
  public void run() {
    while (this.alive) {
      try {
        Cowboy target = bar.pickCowboy(this.getName());
        if (target == null) {
          break;
        }
        System.out.printf("%s shoot %s on %s damage%n", this.getName(), target.getName(), this.getDamage());
        boolean success = target.tageDamage(this);
        if (success) {
            this.sleep(1000);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void enterBar(Bar bar) {
      this.bar = bar;
  }

  public synchronized boolean tageDamage(Cowboy shooter) {
    if(!this.alive) {
      System.out.printf("Jeebers, someone got %s first%n", this.getName());
      return false;
    }
    if(!shooter.isCowboyAlive()) {
      System.out.printf("Shooter died first, %s lucky day%n", shooter.getName());
      return false;
    }
    this.health = this.health - shooter.getDamage();
    System.out.printf("%s has %s health left%n", this.getName(), this.health);
    if (this.health <= 0) {
      this.alive = false;
      bar.removeCorpse(this);
      System.out.printf("Cowboy %s died%n", this.getName());
    }
    return true;
  }

  public int getDamage() {
    return damage;
  }


  public boolean isCowboyAlive() {
    return alive;
  }
}