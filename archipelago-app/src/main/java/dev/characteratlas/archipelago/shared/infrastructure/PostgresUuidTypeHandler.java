package dev.characteratlas.archipelago.shared.infrastructure;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/** PostgreSQLのUUIDの型の変換処理を表す。 */
@MappedTypes(UUID.class)
public final class PostgresUuidTypeHandler extends BaseTypeHandler<UUID> {
  /** setのNonのNullのパラメーターを扱う。 */
  @Override
  public void setNonNullParameter(PreparedStatement statement, int index, UUID value, JdbcType type)
      throws SQLException {
    statement.setObject(index, value);
  }

  /** 取得するの値の欠如を許すの結果を扱う。 */
  @Override
  public UUID getNullableResult(ResultSet result, String column) throws SQLException {
    return result.getObject(column, UUID.class);
  }

  /** 取得するの値の欠如を許すの結果を扱う。 */
  @Override
  public UUID getNullableResult(ResultSet result, int index) throws SQLException {
    return result.getObject(index, UUID.class);
  }

  /** 取得するの値の欠如を許すの結果を扱う。 */
  @Override
  public UUID getNullableResult(CallableStatement statement, int index) throws SQLException {
    return statement.getObject(index, UUID.class);
  }
}
