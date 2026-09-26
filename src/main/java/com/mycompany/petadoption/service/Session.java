package com.mycompany.petadoption.service;

/** An opaque identity issued only by AuthService. */
public final class Session {
  final long userId;

  Session(long userId) {
    this.userId = userId;
  }
}
