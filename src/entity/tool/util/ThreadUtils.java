/**
 *
 *  License: http://www.apache.org/licenses/LICENSE-2.0
 *  Home page: https://github.com/linlurui/entityQueryable
 *  Note: to build on java, include the jdk1.6+ compiler symbol (and yes,
 *  I know the difference between language and runtime versions; this is a compromise).
 * @author linlurui
 * @Date Date: 2017-09-09
 */

package entity.tool.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ThreadUtils
{
    public static final int DontWaitting = 0;
    public static final int JustOneResultToEnd = 1;
    public static final int JustTowResultToEnd = 2;
    public static final int JustThreeResultToEnd = 3;
    public static final int WaitAllToEnd = 4;
	    
	// 创建线程池（使用了预定义的配置）
	private static final ExecutorService executor = Executors.newFixedThreadPool(20);

    private static final Logger log = LoggerFactory.getLogger(ThreadUtils.class);
    
    public static <T> List<T> sync(final Runnable ...tasks) {
        
        return execute(WaitAllToEnd, 0, null, tasks);
    }
    
    public static <T> List<T> async(final Runnable ...tasks) {
        
        return execute(DontWaitting, 0, null, tasks);
    }
    
    public static <T> List<T> onec(final Runnable ...tasks) {
        
        return execute(JustOneResultToEnd, 0, null, tasks);
    }
    
    public static <T> List<T> sync(final Callable<List<T>> ...tasks) {
        
        return execute(WaitAllToEnd, 0, null, tasks);
    }
    
    public static <T> List<T> async(final Callable<List<T>> ...tasks) {
        
        return execute(DontWaitting, 0, null, tasks);
    }
    
    public static <T> List<T> onec(final Callable<List<T>> ...tasks) {
        
        return execute(JustOneResultToEnd, 0, null, tasks);
    }

    public static <T> List<T> execute(final int howToEnd, final int maxThread, final Callback<List<T>> callback, Runnable ...tasks) {
        
        if(tasks == null){
            return null;
        }
        
        List<Callable<List<T>>> calls = new CopyOnWriteArrayList<Callable<List<T>>>();
        for(Runnable task : tasks) {
            calls.add( Executors.callable(task, ((List<T>)new CopyOnWriteArrayList<T>())) );
        }
        
        return execute(howToEnd, maxThread, callback, calls);
    }

    public static <T> List<T> sync(final List<Callable<List<T>>> tasks) {
        
        return execute(WaitAllToEnd, 0, null, tasks);
    }
    
    public static <T> List<T> async(final List<Callable<List<T>>> tasks) {
        
        return execute(DontWaitting, 0, null, tasks);
    }
    
    public static <T> List<T> onec(final List<Callable<List<T>>> tasks) {
        
        return execute(JustOneResultToEnd, 0, null, tasks);
    }

    public static <T> List<T> execute(final int howToEnd, final int maxThread, final Callback<List<T>> callback, final Callable<List<T>> ...tasks) {
        List<Callable<List<T>>> calls = new CopyOnWriteArrayList<Callable<List<T>>>();
        
        for(Callable<List<T>> task : tasks) {
            calls.add( task );
        }
        
        return execute(howToEnd, maxThread, callback, calls);
    }
    
    public static <T> List<T> execute(final int howToEnd, final int maxThread, final Callback<List<T>> callback, final List<Callable<List<T>>> tasks) {
        
        if(tasks == null){
            return null;
        }
        
        @SuppressWarnings( {"unchecked", "rawtypes"} )
        final List<T> results = Collections.synchronizedList(new CopyOnWriteArrayList());
        
        final List<FutureTask<List<T>>> futureList = new CopyOnWriteArrayList<FutureTask<List<T>>>();

        try {
            
            for(Callable<List<T>> task : tasks)
            {
                FutureTask<List<T>> future = new FutureTask<List<T>>(task) {
                    
                    // 异步任务执行完成，回调
                    @Override
                    protected void done() {
                        try {
                        	if(this.isCancelled()) {
                        		return;
                        	}
                        	
                            List<T> list = null;
                            list = get();
                            if(list != null && list.size() > 0) {
                                
                                if( callback != null ) {
                                    callback.setData( list );
                                    executor.execute( callback );
                                }
                            }

                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            
                        }
                    }
                };
                
                futureList.add(future);
                executor.submit(future);
            }
    
            howToEndHandle( howToEnd, results, futureList );
            
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        
        return results;
    }

    private static <T> void howToEndHandle( final int howToEnd,
            final List<T> results, final List<FutureTask<List<T>>> futureList )
    {
        if(howToEnd > DontWaitting) {
            for(FutureTask<List<T>> future : futureList) {
                try
                {
                    if(JustOneResultToEnd == howToEnd) {
                        if(results.size() > 0) {
                            cancel(future);
                            continue;
                        }
                    }
                    
                    if(JustTowResultToEnd == howToEnd) {
                        if(results.size() > 1) {
                            cancel(future);
                            continue;
                        }
                    }
                    
                    if(JustThreeResultToEnd == howToEnd) {
                        if(results.size() > 2) {
                            cancel(future);
                            continue;
                        }
                    }
                    
                    List<T> items = future.get();
                    // TODO
                    if(CollectionUtils.isNotEmpty(items)) {
                    	results.addAll(items);
                    }
                    
                } catch ( Exception e )
                {
                    log.error(e.getMessage(), e);
                } 
            }
        }
    }
    
    public static <T> void cancel( FutureTask<T> future )
    {
        if(!future.isDone()) {
            future.cancel(true);
        }
    }
    
    public static <T> void cancel( List<FutureTask<T>> futureList )
    {
        for(FutureTask<T> future : futureList) {
            cancel(future);
        }
        
    }
}
