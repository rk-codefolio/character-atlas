package dev.characteratlas.archipelago.moderation.application;

import java.util.List;

/** 管理対象のPostsを表す。 */
public interface ModeratedPosts {
  /** 指定された条件に沿って一覧を取得する。 */
  List<PostEntry> list(int offset);
}
