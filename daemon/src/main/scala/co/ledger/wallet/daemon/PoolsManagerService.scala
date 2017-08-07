/*
 * MIT License
 *
 * Copyright (c) 2017 Ledger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package co.ledger.wallet.daemon

import java.sql.Timestamp
import java.util.Date
import java.util.concurrent.{Executor, Executors, ThreadPoolExecutor}

import scala.concurrent.{ExecutionContext, Future}

class PoolsManagerService {
  private implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  import LedgerWalletDaemon.profile.api._
  import co.ledger.wallet.daemon.database._

  def start(): Future[Unit] = database map {(database) =>

  }

  def stop(): Future[Unit] = synchronized {
    if (_database.nonEmpty) {
      _database = None
    }
    Future.successful()
  }

  def createPool(name: String): Future[Unit] = database flatMap {(db) =>
    db.run(pools.filter(_.name === name).size.result).map((_, db))
  } flatMap {case (size, db) =>
    if (size == 0) {
      db.run(DBIO.seq(
        pools += (name, new Timestamp(new Date().getTime))
      ))
    } else {
      throw new RuntimeException(s"Pool '$name' already exists")
    }
  }

  def database = synchronized {
    _database match {
      case None =>
        _database = Some(Future {
          Database.forConfig(LedgerWalletDaemon.profileName)
        } flatMap migrate)
      case Some(d) =>
        d
    }
    _database.get
  }

  private def migrate(db: Database): Future[Database] = {
    // Fetch database version
    db.run(databaseVersions.sortBy(_.version.desc).map(_.version).take(1).result) map {(result) =>
      result.head
    } recover {
      case all => -1
    } flatMap {(currentVersion) =>
      val maxVersion = Migrations.keys.toArray.sortWith(_ > _).head
      def migrate(version: Int): Future[Unit] = {
        if (version > maxVersion)
          Future.successful()
        else {
          val script = DBIO.seq(
            Migrations(version),
            databaseVersions += (version, new Timestamp(new Date().getTime))
          )
          db.run(script.transactionally) map {(_) =>
            migrate(version + 1)
          }
        }
      }
      migrate(currentVersion + 1)
    } map {(_) =>
      db
    }
  }

  private var _database: Option[Future[Database]] = None
}