package com.zhuhean.annotation.demo;

import java.lang.String;

public final class UserBuilder {
  private String firstName;

  private String lastName;

  private String nickName;

  private int age;

  public UserBuilder firstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public UserBuilder lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public UserBuilder nickName(String nickName) {
    this.nickName = nickName;
    return this;
  }

  public UserBuilder age(int age) {
    this.age = age;
    return this;
  }

  public User build() {
    User user = new User();
    user.firstName = firstName;
    user.lastName = lastName;
    user.nickName = nickName;
    user.age = age;
    return user;
  }
}
