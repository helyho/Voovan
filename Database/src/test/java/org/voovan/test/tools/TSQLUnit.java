package org.voovan.tools;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TSQLUnit {

    public static void main(String[] args) {
        String s = "select * from deposit_request m1 \n" +
                " , (select 1 from adfa where mmmm=::111 and kkkk=::kkk)m2, gggg m3 \n" +
                "                            where status != '1111) ooo' \n" +
                "                            and  kkk+ppp > ::0 \n" +
                "                            and  (kkk+ppp) > ::0 \n" +
                "                            and onceDrawLimit = ::onceDrawLimit \n" +
                "                            AND `t1.random_deposit_amountx` = ::kkand \n" +
                "                            AND `random_deposit_amountx` = ::kkand \n" +
                "							 and  mmm not in (::10, ::11, ::12, ::13)\n" +
                "							 and  mmm in (::10, ::11, ::12, ::13)\n" +
                "                            and  (client_account_name >= ::1 or client_card_number <= ::2) \n" +
                "                            and  (client_account_name != ::3 or client_card_numbe1 > ::4) \n" +
                "                            and  (post_script >= ::6 or deposit_amount = ::7 or random_deposit_amount = ::8)\n" +
                "                            and  (select * from xx where kk=::6)\n" +
                "                            and  exists (select * from xx where kk=::6)\n" +
                "                            and  not exists (select * from xx where kk=::6)\n" +
                "                            and `user_name` like CONCAT('!%', CONCAT( ::userName,'%')) \n"+
                "                            and `ip` like CONCAT('%', CONCAT( ::ip,'%')) \n"+
                "                            and state = 1 limit 10, 100";

//		String s = "SELECT count(0) from 90_entrust where 1=1 and status in (0,1,2,3) and user_id = '7hJvVarCrMe' and original_market_id = '90' and type in (0,1) and xx is null";
//		try {
//			DBAccess.newInstance().queryObject(s, String.class);
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}

        System.out.println(
                TEnv.measureTime(()->{
                    System.out.println(s);
                    return TSQL.removeEmptyCondiction(s, TObject.asMap("1", null, "4", null, "7", null, "10", null, "11", null, "userName", 111));
                })
        );


//        s = "select * from ec_menu em where 1=1 \n" +
//                "                        and `menu_id`=::menuId \n" +
//                "                        and `t1.menu_name` like CONCAT('%', CONCAT( ::menuName,'%')) \n" +
//                "                        and em.state=1 and em.parent_menu_id=0 limit ::pageStart , ::pageSize";
//
//        System.out.println(s);
//        System.out.println(TSQL.removeEmptyCondiction(s, TObject.asMap("menuId", "mid", /*"menuName", "mN",*/ "pageStart", 2, "pageSize", 100)));
    }
}
