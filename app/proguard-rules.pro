# Room keeps generated implementations reachable via reflection-free entry points,
# but these keep the release build safe if R8 is enabled later.
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** create(android.content.Context, java.lang.String, android.content.Context);
}
-dontwarn androidx.room.paging.**
